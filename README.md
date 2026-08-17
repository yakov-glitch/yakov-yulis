# תזכורת נוכחות — Android

אפליקציית Android שמגדירה Geofence סביב הבית ושולחת התראה בעת כניסה/יציאה.

## מה יש בגרסה 1.0
- בחירת הבית לפי המיקום הנוכחי, או הזנה ידנית של Latitude/Longitude.
- רדיוס 100–2000 מטר.
- התראת כניסה והתראת יציאה, כל אחת ניתנת להפעלה/כיבוי.
- קישור אופציונלי לשעון נוכחות שנפתח בלחיצה על ההתראה.
- שמירת ההגדרות מקומית בלבד.
- שחזור Geofence אחרי אתחול הטלפון או עדכון האפליקציה.
- כפתור בדיקת התראה.

## הרשאות חובה בטלפון
1. מיקום מדויק.
2. מיקום: "אפשר כל הזמן" (Android 10+).
3. התראות (Android 13+).

## בנייה ב-Android Studio
פתח את התיקייה כפרויקט, המתן ל-Gradle Sync, ואז:
Build > Build APK(s)

ה-APK יופיע תחת:
`app/build/outputs/apk/debug/app-debug.apk`

## בנייה אוטומטית ב-GitHub
קובץ workflow מצורף תחת `.github/workflows/build-apk.yml`.
לאחר העלאת הפרויקט ל-GitHub, עבור ל-Actions > Build Android APK > Run workflow.
בסיום, הורד את artifact בשם `attendance-reminder-debug-apk`.

## הערות שימוש
Geofencing תלוי ב-Google Play services ובמדיניות החיסכון בסוללה של יצרן המכשיר. אירוע כניסה/יציאה עשוי להגיע בעיכוב. מומלץ להתחיל ברדיוס 200–300 מטר.
