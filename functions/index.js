const functions = require("firebase-functions/v2");
const scheduler = require("firebase-functions/v2/scheduler");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

exports.sendUpcomingClassNotifications = scheduler.onSchedule("every 60 minutes", async (event) => {
  console.log('sendUpcomingClassNotifications triggered');
  
  const now = admin.firestore.Timestamp.now();
  const twelveHoursFromNow = admin.firestore.Timestamp.fromMillis(now.toMillis() + 12 * 60 * 60 * 1000);

  try {
    const bookingsSnapshot = await db.collection('bookings').get();
    const lessonsToNotify = [];

    for (const bookingDoc of bookingsSnapshot.docs) {
      const { userId, lessonId } = bookingDoc.data();
      console.log(`Checking booking for userId: ${userId}, lessonId: ${lessonId}`);

      const lessonDoc = await db.collection('lessons').doc(lessonId).get();
      if (!lessonDoc.exists) {
        console.log(`Lesson document not found for lessonId: ${lessonId}`);
        continue;
      }

      const lesson = lessonDoc.data();
      const lessonTime = admin.firestore.Timestamp.fromDate(new Date(`${lesson.schedule.date}T${lesson.schedule.time}:00`));
      const diff = Math.abs(lessonTime.toMillis() - twelveHoursFromNow.toMillis());
      console.log(`Lesson time diff: ${diff} ms`);

      const isIn12Hours = diff < 30 * 60 * 1000;
      if (isIn12Hours) {
        lessonsToNotify.push({ userId, lesson });
        console.log(`Added notification for userId: ${userId}, lesson: ${lesson.title}`);
      }
    }

    const tokensSnapshot = await db.collection('users').get();
    const tokenMap = {};
    tokensSnapshot.forEach(doc => {
      const { fcmToken } = doc.data();
      if (fcmToken) {
        tokenMap[doc.id] = fcmToken;
      }
    });

    for (const { userId, lesson } of lessonsToNotify) {
      const token = tokenMap[userId];
      if (!token) {
        console.log(`No FCM token found for userId: ${userId}, skipping notification`);
        continue;
      }

      console.log(`Sending notification to userId: ${userId}`);
      await admin.messaging().send({
        token: token,
        notification: {
          title: 'Reminder: Pilates class in 12 hours!',
          body: `${lesson.title} at ${lesson.schedule.time} on ${lesson.schedule.date}`
        }
      });
      console.log(`Notification sent to userId: ${userId}`);
    }

  } catch (error) {
    console.error('Error in sendUpcomingClassNotifications:', error);
  }

  return null;
});

exports.sendBookingCancelledNotification = functions.https.onCall(async (data, context) => {
console.log('Raw data object:', data);

  const payload = data.data || data;  

  console.log('Payload:', payload);

  const { userId, lessonTitle, lessonDate, lessonTime } = payload;

  console.log('Parsed values:', { userId, lessonTitle, lessonDate, lessonTime });

  if (!userId || typeof userId !== 'string' || userId.trim() === '') {
    console.error('Invalid or missing userId:', userId);
    throw new functions.https.HttpsError('invalid-argument', 'Invalid or missing userId');
  }
  if (!lessonTitle || !lessonDate || !lessonTime) {
    console.error('Missing lesson details:', { lessonTitle, lessonDate, lessonTime });
    throw new functions.https.HttpsError('invalid-argument', 'Missing lesson details');
  }

  try {
    const userDoc = await db.collection('users').doc(userId).get();

    if (!userDoc.exists) {
      console.error('User not found:', userId);
      throw new functions.https.HttpsError('not-found', 'User not found');
    }

    const fcmToken = userDoc.get('fcmToken');
    if (!fcmToken) {
      console.error('User has no FCM token:', userId);
      throw new functions.https.HttpsError('failed-precondition', 'User has no FCM token');
    }

    const message = {
      token: fcmToken,
      notification: {
        title: 'Booking Cancelled',
        body: `Your booking for ${lessonTitle} on ${lessonDate} at ${lessonTime} has been cancelled.`
      }
    };

    await admin.messaging().send(message);
    console.log('Notification sent successfully to user:', userId);
    return { success: true };

  } catch (error) {
    console.error('Failed to send notification:', error);
    throw new functions.https.HttpsError('internal', 'Failed to send notification');
  }
});
