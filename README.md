<img width="1280" height="960" alt="WhatsApp Image 2026-05-28 at 11 48 28 AM" src="https://github.com/user-attachments/assets/68be4584-3721-4e6f-849c-7c577ac5fe0f" />
QR Vault+
تطبيق أندرويد لمسح رموز QR، إنشائها، وحفظها داخل خزنة مشفرة.

ما يفعله التطبيق
مسح رموز QR
مسح رموز QR والباركود عبر الكاميرا
عرض تاريخ عمليات المسح
نسخ ومشاركة النتائج
إنشاء رموز QR
نص أو رابط URL — أي نص أو رابط
شبكة WiFi — اسم الشبكة وكلمة المرور ونوع الحماية (WPA/WEP/None)
حفظ الرمز كصورة PNG ومشاركته
الخزنة المشفرة
قفل المحتوى برمز PIN رباعي
دعم البصمة والتعرف على الوجه (بحسب الجهاز)
تشفير البيانات داخل الجهاز
رمز استرداد احتياطي لاسترجاع الوصول عند نسيان PIN
تصوير تلقائي عند إدخال PIN خاطئ 3 مرات متتالية
النسخ الاحتياطي
تصدير قاعدة البيانات كملف مشفر محلي
رفع النسخة الاحتياطية على Google Drive الخاص بالمستخدم
تذكير بالنسخ الاحتياطي كل 15 يوماً (يبدأ بعد 15 يوماً من التثبيت)
ويدجت
ويدجت بسيط على الشاشة الرئيسية للوصول السريع للمسح والإنشاء والخزنة
المواصفات التقنية
البند	القيمة
اللغة	Kotlin
الواجهة	Jetpack Compose
الحد الأدنى لـ Android	API 24 (Android 7.0)
الإصدار المستهدف	API 36
قاعدة البيانات	Room (SQLite)
المسح	ML Kit Barcode Scanning + CameraX
الإنشاء	ZXing
المصادقة	Firebase Google Sign-In
النسخ الاحتياطي السحابي	Google Drive API v3
الإصدار	1.0
متطلبات التشغيل
Android Studio Hedgehog أو أحدث
JDK 17
ملف google-services.json من Firebase (مطلوب لتسجيل الدخول بـ Google)
تفعيل Google Drive API في Google Cloud Console
تشغيل المشروع
bash

git clone https://github.com/akarim-maamri/qr-vault-plus.git
cd qr-vault-plus
# ضع ملف google-services.json داخل مجلد app/
./gradlew assembleDebug
ملاحظات
التطبيق لا يرسل أي بيانات لخوادم خارجية — كل شيء محلي أو على Drive الشخصي للمستخدم
ملف google-services.json لم يُرفع في المستودع لأسباب أمنية، يجب إنشاؤه من Firebase Console
سياسة الخصوصية: https://sites.google.com/view/qrcodescanner-pp/home
المطور
MAAMRI ABDELKARIM
Jussor.Tech@gmail.com

https://github.com/akarim-maamri
