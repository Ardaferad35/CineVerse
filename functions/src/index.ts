import { initializeApp } from "firebase-admin/app";
import { getFirestore, DocumentData } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import { onDocumentCreated, onDocumentUpdated } from "firebase-functions/v2/firestore";
import { logger } from "firebase-functions/v2";

initializeApp();
const db = getFirestore();
const messaging = getMessaging();

/**
 * Bir bildirim, hangi ekrana gitmesi gerektiğini Android tarafındaki
 * CineVerseNavGraph'taki tıklama yönlendirmesiyle AYNI mantıkla belirler
 * (bkz. CineVerseNavGraph.kt — bildirim tıklama switch'i). Buradaki route
 * string'i, cihazdaki LocalNotificationHelper.show() çağrısına aynı deep-link
 * formatında geçiyor.
 */
function routeForNotification(notification: DocumentData): string {
  const type = notification.type as string | undefined;
  if (type === "friend_request" || type === "friend_request_accepted") {
    return "friends";
  }
  if (notification.tvId != null || notification.mediaType === "tv") {
    const id = notification.tvId ?? notification.movieId;
    return id != null ? `tv_detail/${id}` : "notifications";
  }
  if (notification.movieId != null) {
    return `movie_detail/${notification.movieId}`;
  }
  return "notifications";
}

/**
 * TÜM bildirim tiplerinin (comment_reply, friend_request,
 * friend_request_accepted, ileride recommendation) tek ortak push
 * gönderme noktası. Yeni bir bildirim tipi eklemek için bu fonksiyona
 * dokunmaya GEREK YOK — sadece ilgili yer bir "notifications" belgesi
 * yazsın yeterli.
 *
 * data-only payload kullanılıyor (notification: bloğu YOK) — aksi halde
 * uygulama arka plandayken Android bildirimi otomatik gösterir ve
 * Android tarafındaki onMessageReceived hiç çağrılmaz, deep-link verisi
 * sessizce kaybolur (bkz. CineVerseMessagingService.kt).
 */
export const onNotificationCreated = onDocumentCreated(
  "users/{uid}/notifications/{notificationId}",
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) return;
    const uid = event.params.uid;
    const notification = snapshot.data();

    const tokensSnapshot = await db.collection("users").doc(uid).collection("fcmTokens").get();
    if (tokensSnapshot.empty) return;
    const tokens = tokensSnapshot.docs.map((doc) => doc.id);

    const route = routeForNotification(notification);
    const response = await messaging.sendEachForMulticast({
      tokens,
      data: {
        notificationId: event.params.notificationId,
        type: String(notification.type ?? ""),
        title: String(notification.title ?? ""),
        body: String(notification.body ?? ""),
        route,
      },
      android: { priority: "high" },
    });

    const staleTokens: string[] = [];
    response.responses.forEach((result, index) => {
      if (result.success) return;
      const code = result.error?.code;
      if (
        code === "messaging/registration-token-not-registered" ||
        code === "messaging/invalid-registration-token"
      ) {
        staleTokens.push(tokens[index]);
      } else {
        logger.warn("FCM gönderimi başarısız", { uid, token: tokens[index], error: result.error?.message });
      }
    });

    await Promise.all(
      staleTokens.map((token) =>
        db.collection("users").doc(uid).collection("fcmTokens").doc(token).delete(),
      ),
    );
  },
);

/** Bir arkadaşlık isteği oluşturulduğunda, alıcıya görünen/push'lanan bildirim belgesini yazar. */
export const onFriendRequestCreated = onDocumentCreated(
  "users/{uid}/friendRequests/{fromUid}",
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) return;
    const uid = event.params.uid;
    const request = snapshot.data();

    await db.collection("users").doc(uid).collection("notifications").add({
      type: "friend_request",
      title: "Yeni arkadaşlık isteği",
      body: `${request.fromFullName || request.fromUsername} sana arkadaşlık isteği gönderdi`,
      movieId: null,
      tvId: null,
      mediaType: null,
      isRead: false,
      createdAt: Date.now(),
    });
  },
);

/**
 * İstemci, kabul ederken artık SADECE friendRequests/{fromUid}.status'u
 * "accepted" yapıyor (bkz. FriendRepository.acceptFriendRequest). Gerçek
 * arkadaşlık ilişkisini kuran iki taraflı yazım burada, Admin SDK ile
 * (firestore.rules'ı bypass ederek) yapılıyor — client'ların "friends/"
 * koleksiyonuna artık doğrudan create izni yok (bkz. firestore.rules).
 */
export const onFriendRequestAccepted = onDocumentUpdated(
  "users/{uid}/friendRequests/{fromUid}",
  async (event) => {
    const change = event.data;
    if (!change) return;
    const before = change.before.data();
    const after = change.after.data();
    if (before.status === after.status || after.status !== "accepted") return;

    const uid = event.params.uid; // isteği kabul eden
    const fromUid = event.params.fromUid; // isteği gönderen

    const [meSnapshot, senderSnapshot] = await Promise.all([
      db.collection("users").doc(uid).get(),
      db.collection("users").doc(fromUid).get(),
    ]);
    const me = meSnapshot.data() ?? {};
    const sender = senderSnapshot.data() ?? {};
    const now = Date.now();

    const myFriendDoc = {
      friendUid: fromUid,
      username: after.fromUsername || sender.username || "",
      fullName: after.fromFullName || sender.fullName || "",
      avatarId: after.fromAvatarId || sender.avatarId || "",
      since: now,
    };
    const theirFriendDoc = {
      friendUid: uid,
      username: me.username || "",
      fullName: me.fullName || "",
      avatarId: me.avatarId || "",
      since: now,
    };

    const batch = db.batch();
    batch.set(db.collection("users").doc(uid).collection("friends").doc(fromUid), myFriendDoc);
    batch.set(db.collection("users").doc(fromUid).collection("friends").doc(uid), theirFriendDoc);
    batch.delete(change.after.ref);
    batch.set(db.collection("users").doc(fromUid).collection("notifications").doc(), {
      type: "friend_request_accepted",
      title: "Arkadaşlık isteğin kabul edildi",
      body: `${me.fullName || me.username || "Bir kullanıcı"} arkadaşlık isteğini kabul etti`,
      movieId: null,
      tvId: null,
      mediaType: null,
      isRead: false,
      createdAt: now,
    });

    await batch.commit();
  },
);
