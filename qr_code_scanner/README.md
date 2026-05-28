<div align="center">

# +QR Vault
### تطبيق أندرويد متكامل لمسح وإنشاء وحماية رموز QR

[![Android](https://img.shields.io/badge/Platform-Android-green?logo=android)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple?logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue?logo=android)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)
[![Version](https://img.shields.io/badge/Version-1.0-cyan)](https://github.com/akarim-maamri)

</div>

---

## 📱 نبذة عن التطبيق
**تطبيق بسيط لأجهزة أندرويد مخصص لمسح رموز QR وإنشائها، مع توفير خيار لحفظها داخل خزنة محلية مؤمنة**

---

## ✨ المميزات الرئيسية

### 📷 مسح رموز QR
- مسح فوري باستخدام كاميرا الجهاز
- دعم جميع أنواع رموز QR والباركود
- تاريخ كامل لجميع عمليات المسح
- نسخ ومشاركة نتائج المسح بسهولة

### 🎨 إنشاء رموز QR مخصصة
- **روابط URL** — مواقع ويب وروابط مباشرة
- **شبكات WiFi** — مشاركة كلمة المرور بنقرة واحدة
- **مواقع GPS** — إحداثيات جغرافية
- **تطبيقات WhatsApp & Telegram** — روابط مباشرة

### 🔐 خزنة الأمان الفائقة
- **تشفير AES-256** لحماية البيانات
- قفل برمز **PIN**
- دعم **البصمة والتعرف على الوجه**
- **رمز استرداد احتياطي** في حالة نسيان PIN
- **رصد أي متطفل** — تلتقط صورة عند محاولة الاختراق وتخزن له صورة محليا داخل الهاتف.
- حفظ أي نوع من الرموز أو الملاحظات داخل الخزنة

### ☁️ النسخ الاحتياطي
- **Google Drive** — نسخ احتياطي تلقائي مشفر
- **ملف محلي** — تصدير واستيراد مشفر
- تذكير تلقائي كل 15 يوماً

### 🔧 الويدجت
- ويدجت سريع على الشاشة الرئيسية
- أزرار مسح، إنشاء، والوصول للخزنة مباشرة

---

## 🛠️ التقنيات المستخدمة

| التقنية | الاستخدام |
|---------|-----------|
| **Kotlin** | لغة البرمجة الرئيسية |
| **Jetpack Compose** | بناء واجهة المستخدم |
| **Room Database** | تخزين البيانات محلياً |
| **ML Kit (Barcode Scanning)** | مسح رموز QR |
| **ZXing** | إنشاء رموز QR |
| **Google Drive API** | النسخ الاحتياطي السحابي |
| **BiometricPrompt** | المصادقة البيومترية |
| **Firebase (Google Sign-In)** | تسجيل الدخول |
| **Glance API** | ويدجت الشاشة الرئيسية |
| **AES-256 Encryption** | تشفير البيانات |

---

## 📂 هيكل المشروع

```
app/src/main/java/com/example/qrcodescanner/
├── data/                        # طبقة البيانات
│   ├── AppDatabase.kt           # قاعدة بيانات Room
│   ├── BiometricHelper.kt       # مساعد البيومتريا
│   ├── DatabaseBackupUtils.kt   # أدوات النسخ الاحتياطي
│   ├── L10n.kt                  # الترجمة (AR/EN)
│   └── ...
├── ui/
│   ├── components/              # مكونات واجهة قابلة لإعادة الاستخدام
│   │   ├── AppInfoDialog.kt     # نافذة معلومات التطبيق
│   │   └── ...
│   ├── main/
│   │   └── MainScreen.kt        # الشاشة الرئيسية
│   └── screens/
│       ├── ScannerScreen.kt     # شاشة المسح
│       ├── GeneratorScreen.kt   # شاشة الإنشاء
│       ├── VaultDashboard.kt    # لوحة الخزنة
│       ├── VaultLockScreen.kt   # شاشة قفل الخزنة
│       ├── VaultSettingsScreen.kt # إعدادات الخزنة
│       └── ...
├── utils/
│   ├── GoogleDriveBackupHelper.kt # مساعد Google Drive
│   └── ...
└── widget/                      # ويدجت الشاشة الرئيسية
```

---

## 🚀 تشغيل المشروع

### المتطلبات
- **Android Studio** Hedgehog أو أحدث
- **JDK 17**
- **Android SDK** API 26+
- حساب **Firebase** مع تفعيل Google Sign-In

### خطوات الإعداد

1. **استنساخ المستودع**
```bash
git clone https://github.com/akarim-maamri/qr-vault-plus.git
cd qr-vault-plus
```

2. **ربط Firebase**
   - أنشئ مشروعاً في [Firebase Console](https://console.firebase.google.com)
   - فعّل **Google Sign-In** في Authentication
   - أضف **SHA-1** fingerprint لتطبيقك
   - حمّل ملف `google-services.json` إلى مجلد `app/`

3. **تفعيل Google Drive API**
   - افتح [Google Cloud Console](https://console.cloud.google.com)
   - فعّل **Google Drive API** للمشروع نفسه

4. **بناء التطبيق**
```bash
./gradlew assembleDebug
```

---

## 📸 لقطات الشاشة

<img width="1080" height="2400" alt="11110000" src="https://github.com/user-attachments/assets/bb9c66a4-0ec0-4ee3-a920-458817e9e686" />
<img width="1080" height="2400" alt="9999" src="https://github.com/user-attachments/assets/2614c0a9-68ec-4297-899f-7714f46ecf34" />
<img width="1080" height="2400" alt="8888" src="https://github.com/user-attachments/assets/8f96c145-e3a6-474c-a0d1-3d36e64a4062" />
<img width="540" height="1200" alt="7777" src="https://github.com/user-attachments/assets/a6a4da19-6f00-4c2f-9820-989cdd21e0f6" />
<img width="540" height="1200" alt="6666" src="https://github.com/user-attachments/assets/55053641-5113-4542-93c4-6d94d08a9a90" />
<img width="540" height="1200" alt="5555" src="https://github.com/user-attachments/assets/286500d6-4616-432e-bd9b-fb68c7601381" />
<img width="1080" height="2400" alt="4444" src="https://github.com/user-attachments/assets/3a720fad-5f26-44d7-b247-5c90b7473316" />
<img width="1080" height="2400" alt="3333" src="https://github.com/user-attachments/assets/40a45a71-36f9-42e9-ad68-82556a694275" />
<img width="540" height="1200" alt="2222" src="https://github.com/user-attachments/assets/d0514c38-4b08-4268-a27f-6d251f0910da" />
<img width="540" height="1200" alt="1111" src="https://github.com/user-attachments/assets/bac48485-385d-46b9-ab5e-e470e840d9af" />


---

## 🔒 الأمان والخصوصية

- **لا يُرسَل أي بيانات** لخوادم خارجية
- جميع البيانات مشفرة بـ **AES-256** داخل الجهاز
- النسخ الاحتياطي على **Drive الشخصي للمستخدم** فقط
- سياسة الخصوصية: [اقرأ هنا](https://sites.google.com/view/qrcodescanner-pp/home)

---

## 📄 الرخصة

```
MIT License — © 2026 MAAMRI ABDELKARIM
```

---

## 👨‍💻 المطور

<div align="center">

**MAAMRI ABDELKARIM**

[![Email](https://img.shields.io/badge/Email-Jussor.Tech@gmail.com-red?logo=gmail)](mailto:Jussor.Tech@gmail.com)
[![Facebook](https://img.shields.io/badge/Facebook-maamri.abdelkarim-blue?logo=facebook)](https://web.facebook.com/maamri.abdelkarim.2025)
[![Telegram](https://img.shields.io/badge/Telegram-jussor__tech-cyan?logo=telegram)](https://t.me/jussor_tech)
[![GitHub](https://img.shields.io/badge/GitHub-akarim--maamri-black?logo=github)](https://github.com/akarim-maamri)

</div>

---

<div align="center">

صُنع بـ ❤️ في الجزائر 🇩🇿

⭐ لا تنسَ إضافة نجمة للمستودع إذا أعجبك التطبيق!

</div>
