# النشر المباشر إلى متجر جوجل

يُرفع «اتصال سريع» من الطرفية عبر واجهة النشر في المتجر، دون فتح لوحة التحكم، بإضافة البناء `com.github.triplet.play` وحساب الخدمة نفسه المستخدم في «نتلو» و«اناء».

## الإعداد الموجود

حساب الخدمة:

```
play-publisher-quran@api-7291268109329898663-2910.iam.gserviceaccount.com
```

صلاحياته على هذا التطبيق وحده: عرض المعلومات والجودة، وتعديل المسودات وحذفها، والإصدار للإنتاج، والإصدار لمسارات الاختبار، وإدارة صفحة المتجر، وإقرارات السياسات، والروابط العميقة.

ملف المفتاح، وهو مستبعد من المستودع مع مفتاح التوقيع:

```
keystore/play-service-account.json
```

**لا ترفعه ولا تشاركه.** وبدونه تتعطل إضافة النشر، ويبقى البناء العادي سليماً. لا يطبع أي سكربت هنا المفتاح الخاص ولا رمز الدخول.

للتحقق من الوصول دون رفع شيء:

```powershell
python tools/check_play_auth.py
```

## قراءة الحالة، دون تغيير

```powershell
python tools/check_play_tracks.py --notes
```

```powershell
python tools/check_play_listing.py
```

## نشر إصدار جديد

١. ارفع `versionCode` و`versionName` في `app/build.gradle.kts`، فالمتجر يرفض رقم بناء مستعملاً.

٢. حدّث ملاحظات الإصدار، والحد 500 حرف لكل لغة:

```
app/src/main/play/release-notes/ar/default.txt
app/src/main/play/release-notes/en-US/default.txt
```

٣. شغّل الاختبارات وافحص الكود:

```powershell
.\gradlew.bat testDebugUnitTest lintRelease
```

٤. ارفع إلى المسار المطلوب. الإعداد الافتراضي الآمن مسودة في الاختبار الداخلي:

```powershell
.\gradlew.bat :app:publishReleaseBundle --max-workers=1 --no-configuration-cache
```

مسارات أخرى. الاختبار المغلق اسمه في الواجهة `alpha`:

```powershell
.\gradlew.bat :app:publishReleaseBundle --max-workers=1 --no-configuration-cache --track alpha --release-status completed
```

```powershell
.\gradlew.bat :app:publishReleaseBundle --max-workers=1 --no-configuration-cache --track production --release-status completed
```

ولنقل إصدار مرفوع من مسار إلى آخر، دون رفعه من جديد:

```powershell
.\gradlew.bat :app:promoteReleaseArtifact --from-track internal --promote-track alpha --release-status draft --max-workers=1 --no-configuration-cache
```

٥. تحقق من النتيجة:

```powershell
python tools/check_play_tracks.py --notes
```

## تحديث صفحة المتجر

المصدر الوحيد هو `store/listing.md` و`store/graphics`. ولّد منهما ملفات الإضافة، ثم ارفعها:

```powershell
python tools/sync_play_listing.py
```

```powershell
.\gradlew.bat :app:publishReleaseListing --max-workers=1 --no-configuration-cache --rerun
```

## ملاحظات من التجربة

- **`--max-workers=1` ضروري لرفع صفحة المتجر:** دونه تصل الصور قبل إنشاء صفحة اللغة الجديدة، فيفشل الرفع بخطأ 404.
- **`--rerun` ضروري بعد أي فشل:** تعدّ الإضافة الرفع منتهياً إن فشلت الخطوة الأخيرة فقط، فتنهي التنفيذ في ثانية دون رفع شيء. تأكد دائماً بأداة `check_play_listing.py`.
- **ما دام التطبيق مسودة:** لا يقبل المتجر من الواجهة إلا إصدارات بحالة مسودة، وصلاحية تعديل المسودات لازمة لتثبيت صفحة المتجر. يُطلق أول إصدار من لوحة التحكم.
- **لا يتم من الطرفية:** نماذج المحتوى، وصفة التاجر، ومجموعات المختبِرين، وطلب الوصول إلى الإنتاج. هذه من لوحة التحكم فقط.
- **مراجعة جوجل:** حالة `inProgress` أو «قيد المراجعة» لا تعني أن التحديث متاح للجمهور بعد.
