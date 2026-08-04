// CineVerse: arkadaşlık isteği/kabulü + yorum yanıtı bildirimleri + FCM push.
//
// NOT: fonksiyonun adı ("friend-push") tarihsel — ilk olarak sadece arkadaşlık
// için yazıldı, sonra yorum yanıtları da buraya taşındı. Deploy edilmiş URL'i
// kırmamak için adı değiştirilmedi.
//
// Firestore, Google'a özel bir tetikleyici mekanizmasına (Cloud Functions)
// sahip; Supabase bunu göremez — bu yüzden mimari burada Cloud
// Functions'takinden FARKLI: Android istemcisi Firestore'a yazdıktan HEMEN
// SONRA bu fonksiyonu kendisi, açıkça HTTP ile çağırıyor (bkz. Android
// tarafında FriendRepository.sendFriendRequest / acceptFriendRequest).
//
// Güvenlik: bu fonksiyon bir Firebase SERVİS HESABI (env secret, asla
// istemciye gönderilmez) kullanarak Firestore'a firestore.rules'ı bypass
// eden yazımlar yapıyor (tıpkı Admin SDK gibi). Bu yüzden her isteğin
// GERÇEKTEN iddia ettiği kullanıcıdan geldiğini doğrulamak ZORUNLU — bunu,
// istemcinin gönderdiği Firebase ID token'ını Google'ın herkese açık
// anahtarlarıyla (JWKS) doğrulayarak yapıyoruz. Sadece Supabase'in anon-key
// kapısından geçmek YETERLİ DEĞİL, çünkü anon key herkese açık/istemciye
// gömülü bir değer.

import { SignJWT, importPKCS8, jwtVerify, createRemoteJWKSet } from "https://esm.sh/jose@5";

const FIREBASE_PROJECT_ID = Deno.env.get("FIREBASE_PROJECT_ID")!;
const FIREBASE_CLIENT_EMAIL = Deno.env.get("FIREBASE_CLIENT_EMAIL")!;
const FIREBASE_PRIVATE_KEY = Deno.env.get("FIREBASE_PRIVATE_KEY")!.replace(/\\n/g, "\n");

const FIRESTORE_BASE = `https://firestore.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/databases/(default)/documents`;

const idTokenJwks = createRemoteJWKSet(
  new URL("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"),
);

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-firebase-id-token, content-type",
};

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

interface VerifiedCaller {
  uid: string;
  email: string | null;
}

/** Çağıranın Firebase ID token'ını doğrular, doğrulanmış kimliğini döner. */
async function verifyFirebaseIdToken(idToken: string): Promise<VerifiedCaller> {
  const { payload } = await jwtVerify(idToken, idTokenJwks, {
    issuer: `https://securetoken.google.com/${FIREBASE_PROJECT_ID}`,
    audience: FIREBASE_PROJECT_ID,
  });
  if (!payload.sub) throw new Error("Token içinde sub (uid) yok");
  return { uid: payload.sub, email: typeof payload.email === "string" ? payload.email : null };
}

// --- Google servis hesabı OAuth2 access token'ı (Firestore REST + FCM için) ---
let cachedToken: { token: string; expiresAt: number } | null = null;

async function getAccessToken(): Promise<string> {
  if (cachedToken && cachedToken.expiresAt > Date.now() + 60_000) {
    return cachedToken.token;
  }
  const privateKey = await importPKCS8(FIREBASE_PRIVATE_KEY, "RS256");
  const now = Math.floor(Date.now() / 1000);
  const assertion = await new SignJWT({
    scope: "https://www.googleapis.com/auth/datastore https://www.googleapis.com/auth/firebase.messaging",
  })
    .setProtectedHeader({ alg: "RS256" })
    .setIssuer(FIREBASE_CLIENT_EMAIL)
    .setSubject(FIREBASE_CLIENT_EMAIL)
    .setAudience("https://oauth2.googleapis.com/token")
    .setIssuedAt(now)
    .setExpirationTime(now + 3600)
    .sign(privateKey);

  const response = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });
  if (!response.ok) {
    throw new Error(`Google OAuth2 token alınamadı: ${response.status} ${await response.text()}`);
  }
  const data = await response.json();
  cachedToken = { token: data.access_token, expiresAt: Date.now() + data.expires_in * 1000 };
  return cachedToken.token;
}

// --- Firestore REST yardımcıları ---
// Firestore REST API her alanı {"stringValue": ...} gibi bir "value wrapper"
// içinde bekler/döner — Android SDK'nın gördüğü düz JSON'a çevirmek için
// küçük bir katman.
type Primitive = string | number | boolean | null;

function toFirestoreFields(obj: Record<string, Primitive>): Record<string, unknown> {
  const fields: Record<string, unknown> = {};
  for (const [key, value] of Object.entries(obj)) {
    if (value === null || value === undefined) fields[key] = { nullValue: null };
    else if (typeof value === "boolean") fields[key] = { booleanValue: value };
    else if (typeof value === "number") fields[key] = { integerValue: String(Math.trunc(value)) };
    else fields[key] = { stringValue: String(value) };
  }
  return fields;
}

interface FirestoreWrappedValue {
  stringValue?: string;
  integerValue?: string;
  booleanValue?: boolean;
}

function fromFirestoreFields(
  fields: Record<string, FirestoreWrappedValue> | undefined,
): Record<string, string> {
  const out: Record<string, string> = {};
  if (!fields) return out;
  for (const [key, wrapped] of Object.entries(fields)) {
    const value = wrapped?.stringValue ?? wrapped?.integerValue ?? wrapped?.booleanValue ?? null;
    if (value != null) out[key] = String(value);
  }
  return out;
}

async function firestoreGet(path: string): Promise<Record<string, string> | null> {
  const token = await getAccessToken();
  const response = await fetch(`${FIRESTORE_BASE}/${path}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (response.status === 404) return null;
  if (!response.ok) throw new Error(`Firestore GET başarısız (${path}): ${response.status}`);
  const data = await response.json();
  return fromFirestoreFields(data.fields);
}

async function firestoreSet(path: string, fields: Record<string, Primitive>): Promise<void> {
  const token = await getAccessToken();
  const response = await fetch(`${FIRESTORE_BASE}/${path}`, {
    method: "PATCH",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: JSON.stringify({ fields: toFirestoreFields(fields) }),
  });
  if (!response.ok) {
    throw new Error(`Firestore SET başarısız (${path}): ${response.status} ${await response.text()}`);
  }
}

async function firestoreAdd(collectionPath: string, fields: Record<string, Primitive>): Promise<void> {
  const token = await getAccessToken();
  const response = await fetch(`${FIRESTORE_BASE}/${collectionPath}`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: JSON.stringify({ fields: toFirestoreFields(fields) }),
  });
  if (!response.ok) {
    throw new Error(`Firestore ADD başarısız (${collectionPath}): ${response.status} ${await response.text()}`);
  }
}

async function firestoreDelete(path: string): Promise<void> {
  const token = await getAccessToken();
  const response = await fetch(`${FIRESTORE_BASE}/${path}`, {
    method: "DELETE",
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok && response.status !== 404) {
    throw new Error(`Firestore DELETE başarısız (${path}): ${response.status}`);
  }
}

async function listFcmTokens(uid: string): Promise<string[]> {
  const token = await getAccessToken();
  const response = await fetch(`${FIRESTORE_BASE}/users/${uid}/fcmTokens`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) return [];
  const data = await response.json();
  const documents = data.documents ?? [];
  return documents.map((doc: { name: string }) => doc.name.split("/").pop() as string);
}

/**
 * data-only FCM payload gönderir ("notification" bloğu YOK) — aksi halde
 * uygulama arka plandayken Android bildirimi otomatik gösterir ve Android
 * tarafındaki onMessageReceived hiç çağrılmaz, deep-link verisi kaybolur
 * (bkz. CineVerseMessagingService.kt).
 */
async function sendPush(uid: string, payload: { type: string; title: string; body: string; route: string }) {
  const tokens = await listFcmTokens(uid);
  if (tokens.length === 0) {
    // Sessizce dönmek, {ok:true} yanıtını bu kullanıcıya HİÇ push
    // gitmediğini gizler — en azından fonksiyon loglarında görünür olsun
    // (bkz. Supabase Dashboard > friend-push > Logs).
    console.warn(`sendPush: uid=${uid} için kayıtlı fcmTokens yok, push atlanıyor`);
    return;
  }
  const accessToken = await getAccessToken();

  await Promise.all(
    tokens.map(async (fcmToken) => {
      const response = await fetch(
        `https://fcm.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/messages:send`,
        {
          method: "POST",
          headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" },
          body: JSON.stringify({
            message: { token: fcmToken, data: payload, android: { priority: "high" } },
          }),
        },
      );
      if (!response.ok) {
        interface FcmErrorDetail {
          "@type"?: string;
          errorCode?: string;
        }
        const errorBody: { error?: { details?: FcmErrorDetail[] } } | null = await response
          .json()
          .catch(() => null);
        const errorCode = errorBody?.error?.details?.find((d) =>
          typeof d["@type"] === "string" && d["@type"]!.includes("FcmError")
        )?.errorCode;
        if (errorCode === "UNREGISTERED" || errorCode === "INVALID_ARGUMENT") {
          await firestoreDelete(`users/${uid}/fcmTokens/${fcmToken}`).catch(() => {});
        } else {
          console.warn("FCM gönderimi başarısız", uid, fcmToken, response.status, errorBody);
        }
      }
    }),
  );
}

interface RequestPayload {
  action: "request";
  targetUid: string;
  fromUid: string;
  fromUsername: string;
  fromFullName: string;
  fromAvatarId: string;
}

interface AcceptPayload {
  action: "accept";
  uid: string;
  fromUid: string;
}

/**
 * Yorum yanıtı. Bildirimin KİME gideceği (targetUid) istemciden ALINMIYOR —
 * sunucu yanıtlanan yorumu Firestore'dan okuyup sahibini kendisi buluyor,
 * aksi halde herkes herkesin bildirim kutusuna istediği metni yazdırabilirdi.
 */
interface CommentReplyPayload {
  action: "comment_reply";
  rootCollection: string; // "movies" | "tv_shows"
  mediaId: number;
  mediaTitle: string;
  parentCommentId: string;
  text: string;
}

type Payload = RequestPayload | AcceptPayload | CommentReplyPayload;

async function handleRequest(payload: RequestPayload, verifiedUid: string): Promise<Response> {
  if (verifiedUid !== payload.fromUid) {
    return jsonResponse({ ok: false, error: "Kimlik uyuşmuyor" }, 403);
  }
  const title = "Yeni arkadaşlık isteği";
  const body = `${payload.fromFullName || payload.fromUsername} sana arkadaşlık isteği gönderdi`;
  await firestoreAdd(`users/${payload.targetUid}/notifications`, {
    type: "friend_request",
    title,
    body,
    movieId: null,
    tvId: null,
    mediaType: null,
    isRead: false,
    createdAt: Date.now(),
  });
  await sendPush(payload.targetUid, { type: "friend_request", title, body, route: "friends" });
  return jsonResponse({ ok: true });
}

async function handleAccept(payload: AcceptPayload, verifiedUid: string): Promise<Response> {
  if (verifiedUid !== payload.uid) {
    return jsonResponse({ ok: false, error: "Kimlik uyuşmuyor" }, 403);
  }
  const [me, sender] = await Promise.all([
    firestoreGet(`users/${payload.uid}`),
    firestoreGet(`users/${payload.fromUid}`),
  ]);
  const now = Date.now();

  await Promise.all([
    firestoreSet(`users/${payload.uid}/friends/${payload.fromUid}`, {
      friendUid: payload.fromUid,
      username: sender?.username ?? "",
      fullName: sender?.fullName ?? "",
      avatarId: sender?.avatarId ?? "",
      since: now,
    }),
    firestoreSet(`users/${payload.fromUid}/friends/${payload.uid}`, {
      friendUid: payload.uid,
      username: me?.username ?? "",
      fullName: me?.fullName ?? "",
      avatarId: me?.avatarId ?? "",
      since: now,
    }),
    firestoreDelete(`users/${payload.uid}/friendRequests/${payload.fromUid}`),
  ]);

  const title = "Arkadaşlık isteğin kabul edildi";
  const body = `${me?.fullName || me?.username || "Bir kullanıcı"} arkadaşlık isteğini kabul etti`;
  await firestoreAdd(`users/${payload.fromUid}/notifications`, {
    type: "friend_request_accepted",
    title,
    body,
    movieId: null,
    tvId: null,
    mediaType: null,
    isRead: false,
    createdAt: now,
  });
  await sendPush(payload.fromUid, { type: "friend_request_accepted", title, body, route: "friends" });
  return jsonResponse({ ok: true });
}

const MAX_TITLE_LENGTH = 150;
const MAX_BODY_LENGTH = 500;
const COMMENT_ROOT_COLLECTIONS = ["movies", "tv_shows"];
// Firestore'un otomatik ürettiği belge ID'leri 20 karakter alfanümeriktir;
// kalıp aynı zamanda REST yoluna "/" veya ".." enjekte edilmesini engelliyor.
const DOCUMENT_ID_PATTERN = /^[A-Za-z0-9_-]{1,64}$/;

async function handleCommentReply(
  payload: CommentReplyPayload,
  caller: VerifiedCaller,
): Promise<Response> {
  if (!COMMENT_ROOT_COLLECTIONS.includes(payload.rootCollection)) {
    return jsonResponse({ ok: false, error: "Geçersiz rootCollection" }, 400);
  }
  if (!Number.isInteger(payload.mediaId) || payload.mediaId <= 0) {
    return jsonResponse({ ok: false, error: "Geçersiz mediaId" }, 400);
  }
  if (!DOCUMENT_ID_PATTERN.test(payload.parentCommentId ?? "")) {
    return jsonResponse({ ok: false, error: "Geçersiz parentCommentId" }, 400);
  }
  const text = (payload.text ?? "").trim();
  if (text.length === 0) {
    return jsonResponse({ ok: false, error: "Yanıt metni boş" }, 400);
  }

  const parentComment = await firestoreGet(
    `${payload.rootCollection}/${payload.mediaId}/comments/${payload.parentCommentId}`,
  );
  if (!parentComment) {
    return jsonResponse({ ok: false, error: "Yanıtlanan yorum bulunamadı" }, 404);
  }
  const targetUid = parentComment.userId;
  if (!targetUid) {
    return jsonResponse({ ok: false, error: "Yorumda userId yok" }, 400);
  }
  // Kendi yorumuna yanıt verene bildirim gitmez. İstemci de bunu kontrol
  // ediyor (bkz. CommentRepository.addReply) ama asıl karar burada.
  if (targetUid === caller.uid) {
    return jsonResponse({ ok: true, skipped: "self_reply" });
  }

  const me = await firestoreGet(`users/${caller.uid}`);
  const displayName = me?.fullName || me?.username || caller.email?.split("@")[0] || "Kullanıcı";
  const isTvShow = payload.rootCollection === "tv_shows";
  const mediaTitle = (payload.mediaTitle ?? "").slice(0, 100);
  const title = `${displayName} yorumunuza yanıt verdi`.slice(0, MAX_TITLE_LENGTH);
  const body = `"${mediaTitle}" ${isTvShow ? "dizisindeki" : "filmindeki"} yorumunuza: ${text.slice(0, 80)}`
    .slice(0, MAX_BODY_LENGTH);
  const route = isTvShow ? `tv_detail/${payload.mediaId}` : `movie_detail/${payload.mediaId}`;

  await firestoreAdd(`users/${targetUid}/notifications`, {
    type: "comment_reply",
    title,
    body,
    movieId: isTvShow ? null : payload.mediaId,
    tvId: isTvShow ? payload.mediaId : null,
    mediaType: isTvShow ? "tv" : "movie",
    isRead: false,
    createdAt: Date.now(),
  });
  await sendPush(targetUid, { type: "comment_reply", title, body, route });
  return jsonResponse({ ok: true });
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const idToken = req.headers.get("x-firebase-id-token");
    if (!idToken) {
      return jsonResponse({ ok: false, error: "X-Firebase-Id-Token eksik" }, 401);
    }
    let caller: VerifiedCaller;
    try {
      caller = await verifyFirebaseIdToken(idToken);
    } catch (error) {
      console.warn("Firebase ID token doğrulaması başarısız:", error);
      return jsonResponse({ ok: false, error: "Geçersiz veya süresi dolmuş token" }, 401);
    }
    const payload = (await req.json()) as Payload;

    if (payload.action === "request") return await handleRequest(payload, caller.uid);
    if (payload.action === "accept") return await handleAccept(payload, caller.uid);
    if (payload.action === "comment_reply") return await handleCommentReply(payload, caller);
    return jsonResponse({ ok: false, error: "Bilinmeyen action" }, 400);
  } catch (error) {
    console.error("friend-push hata:", error);
    return jsonResponse({ ok: false, error: String(error) }, 500);
  }
});
