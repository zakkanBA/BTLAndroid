# BTLAND setup guide

Tai lieu nay dung de khop lai phan Google Maps va Firebase Storage sau khi da sua code.

## 1. Thong tin dang dung trong source

- Android package: `com.example.btland`
- Firebase project id: `btland-4085e`
- Storage bucket trong `app/src/google-services.json`: `btland-4085e.firebasestorage.app`
- Debug SHA1 da kiem tra bang `.\gradlew.bat signingReport`:
  `88:D1:BD:4E:DA:1A:B3:74:9D:65:BB:33:FD:B1:91:7D:CB:78:0D:16`

## 2. Firebase Storage phai dung du

1. Mo Firebase Console cua project `btland-4085e`.
2. Vao `Build > Storage` va bam `Get started` neu Storage chua duoc tao.
3. Chon dung bucket dang dung cho project. App nay dang doc bucket `btland-4085e.firebasestorage.app`.
4. Publish file `storage.rules` trong repo len Firebase Storage Rules.
5. Publish file `firestore.rules` trong repo len Firestore Rules.
6. Dam bao ban dang dang nhap trong app truoc khi upload, vi rules hien tai chi cho user da dang nhap ghi du lieu.
7. Chi giu file `app/src/google-services.json` trong app module. File trung lap trong `app/src/main/java/com/example/btland/google-services.json` da duoc xoa.

## 3. Neu upload van loi

- Loi `Bucket not found`: bucket chua duoc tao, sai project, hoac `google-services.json` khong dung voi project hien tai.
- Loi `Not authorized`: ban chua publish `storage.rules`, hoac dang upload khi chua dang nhap.
- Loi `Project not found`: app dang tro sai Firebase project.
- Loi `Retry limit exceeded`: Internet yeu, VPN chan, hoac Firebase Storage chua san sang.

## 4. Google Maps khong hien thi

1. Mo Google Cloud Console cua cung project voi Firebase.
2. Bat `Maps SDK for Android`.
3. Bat `Billing` cho project.
4. Kiem tra API key trong `app/src/main/AndroidManifest.xml` phai thuoc dung project.
5. Neu API key dang bi restrict, hay them:
   - Package name: `com.example.btland`
   - SHA1: `88:D1:BD:4E:DA:1A:B3:74:9D:65:BB:33:FD:B1:91:7D:CB:78:0D:16`
6. Tren may that, bat Internet, Google Play Services va quyen vi tri cho app.

## 5. Cach test nhanh sau khi cau hinh

1. Dang nhap vao app.
2. Vao tao bai dang, chon it nhat 1 anh, bam dang bai.
3. Neu upload thanh cong, trong Firebase Storage se co thu muc `posts/<postId>/...`.
4. Vao tab `Ban do`.
5. Neu da cap quyen GPS, app uu tien bai dang trong 3km; neu chua cap quyen hoac khong lay duoc GPS, app van hien tat ca bai dang da co toa do.

## 6. Khi doi may tinh hoac keystore

Neu ban build tren may khac, hay chay lai:

```powershell
.\gradlew.bat signingReport
```

Sau do cap nhat SHA1 moi vao Firebase/Google Cloud neu API key dang gioi han theo ung dung Android.
