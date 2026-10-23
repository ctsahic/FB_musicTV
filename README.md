# 📻 רדיו־בוקס TV — RadioBox TV

אפליקציית רדיו ופודקאסטים לאנדרואיד TV. עברית מלאה, RTL, ממשק שלט (D-pad).

## יכולות

- **תחנות רדיו מכל העולם** (מקור: radio-browser.info) מסודרות לפי סגנונות: אלקטרונית, קלאסית, חדשות, ישראלי, פופ, רוק, ג'אז, מוזיקת עולם ואזוריות
- **דירוג חכם לפי רלוונטיות** — תחנה שמסמנת עשרות תגיות (למשל "music, news, rock, pop, jazz") לא תציף יותר את כל הקטגוריות; מכניסים קודם תחנות שהתג המרכזי של הקטגוריה באמת נמצא אצלן, וכפילויות של אותו סטרים **או אותו שם** ממוזגות
- **הסתרת תחנות** — לחיצה ארוכה (או מקש ☰ בשלט) על כרטיס תחנה מסתירה אותה מכל הקטגוריות; שחזור דרך ההגדרות
- **בחירת מדינה** עם חיפוש ודגלים — ההעדפה נשמרת
- **מועדפים** קבועים לתחנות ולפודקאסטים (לב ❤️ על כל כרטיס)
- **סינון איכות** — רק תחנות מעל קצב סיביות נבחר (128/192/320, ברירת מחדל 128+)
- **פודקאסטים בעברית** (מקור: iTunes) — חיפוש דרך כפתור "חיפוש", צ'יפי קטגוריות, ונגן מלא: השהיה, דילוג ±15 שניות, מהירות 0.75x–2x
- **נגינה ברקע** — שירות מדיה בחזית (foreground service) עם MediaSession: הנגינה ממשיכה גם במסך הבית של הטלוויזיה וגם תחת שומר המסך, ומקש המדיה בשלט עוצר/מפעיל מכל מקום
- **באפר גדול לרציפות** (60–120 שניות, ניתן להגדרה עד 180) + התחברות מחדש אוטומטית עם Backoff
- **סטרימינג בלבד** — לא נשמרים קבצי שמע על המכשיר
- **שומר מסך נע** — שעון גדול עם סחיפה איטית (מונע צריבת פיקסלים), מידע "מנגן כעת", אוטומטי אחרי חוסר פעילות (ניתן לכיוון/כיבוי) + הפעלה יזומה; כל מקש מחזיר
- **BACK חכם** — חוזר מסך אחורה; במסך הראשי נפתח דיאלוג יציאה: בטל / ברקע / אישור

## התקנה על הטלוויזיה (sideload)

1. בנה או קח את ה-APK המוכן: `app/build/outputs/apk/release/app-release.apk` (חתום, מוכן להתקנה)
2. הפעל "מקורות לא ידועים" בטלוויזיה: הגדרות → הגנה והגבלות (או הגדרות מפתחים)
3. התקן באחת מהדרכים:
   - **adb** (טלוויזיה ומחשב באותה רשת):
     ```bash
     adb connect <כתובת-IP-של-הטלוויזיה>:5555
     adb install app-release.apk
     ```
   - **קובץ**: העבר את ה-APK לכונן USB, פתח עם סייר קבצים בטלוויזיה והתקן

## פיתוח

```bash
# בנייה (Windows: הגדר JAVA_HOME ל-JBR של Android Studio)
./gradlew :app:assembleDebug      # debug APK
./gradlew :app:assembleRelease    # release APK (חתום עם מפתח debug להתקנה אישית)
./gradlew :app:testDebugUnitTest  # טסטים

# אמולטור Android TV
emulator -avd My_Skyworth
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Deep-link לבדיקות
```bash
adb shell am start -n com.radiobox.tv/.MainActivity --es open podcasts   # favorites / settings
adb shell am start -n com.radiobox.tv/.MainActivity --es open stations --es genre rock
```

## ארכיטקטורה

- **UI**: Jetpack Compose for TV (`androidx.tv:tv-material`), ניווט בערימה פשוטה (`NavState`), RTL כפוי
- **נגינה**: Media3 `MediaSessionService` + `ExoPlayer` (DefaultLoadControl, ICY metadata, cross-protocol redirects)
- **נתונים**: Retrofit + kotlinx-serialization (Radio Browser, iTunes), XmlPullParser ל-RSS, DataStore להעדפות ומועדפים
- **מבנה**: `core/` עזריים · `data/` API+repo+rss · `playback/` שירות וחיבור · `ui/` מסכים ורכיבים

## מקורות תוכן

- תחנות: [radio-browser.info](https://www.radio-browser.info/) — קהילתי, חינמי
- **רשימת seed ישראלית** (`core/IsraeliSeed.kt`): 47 תחנות ישראליות עם סטרימים שאומתו בפועל
  (כאן ב'/ג'/88/קול המוסיקה/מורשת, גלגלצ, גלצ, גלי ישראל, 103FM, לב המדינה, רדיו צחוק, רדיו סהר
  ועוד). הז'אנר שלהן נקבע על ידי האפליקציה — לא על ידי הקהילה — והן מדורגות ראשונות בכל
  קטגוריה רלוונטית ובמסך הבית; תאומים מה-API נזרקים לפי streamKey/שם מנורמל/כינויים.
- פודקאסטים: iTunes Search API — מטא-דאטה + פידי RSS ציבוריים
- ייצוא לצפייה/ניתוח: `python tools/export_stations.py` →
  `radio-browser-il-stations.html` + `.json` (seed + API, ללא כפילויות)
