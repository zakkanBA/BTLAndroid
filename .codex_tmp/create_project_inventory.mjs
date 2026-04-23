import fs from "node:fs/promises";
import path from "node:path";
import { Workbook, SpreadsheetFile } from "@oai/artifact-tool";

const repoRoot = "D:/workspace/adrstudio/BTLAndroid";
const outputDir = path.join(repoRoot, "outputs", "project_docs_20260423");
const workbookPath = path.join(outputDir, "Tai-lieu-cau-truc-du-an-BTLAND.xlsx");
const summaryPreviewPath = path.join(outputDir, "preview_tong_quan.png");
const detailPreviewPath = path.join(outputDir, "preview_danh_muc.png");

const fixedFiles = [
  ".gitignore",
  "build.gradle.kts",
  "settings.gradle.kts",
  "gradle.properties",
  "gradlew",
  "gradlew.bat",
  "firestore.rules",
  "storage.rules",
  "FIREBASE_MAPS_SETUP.md",
  "app/.gitignore",
  "app/build.gradle.kts",
  "app/proguard-rules.pro",
  "app/src/google-services.json",
  "app/src/main/AndroidManifest.xml",
];

const explicit = new Map([
  [".gitignore", note("Cấu hình Git", "Các mẫu bỏ qua file sinh tự động, file build và cấu hình máy.", "Giữ repository sạch, tránh commit rác.", "app/.gitignore, build/")],
  ["build.gradle.kts", note("Cấu hình gốc", "Khai báo pluginManagement và repository dùng cho toàn project.", "Điểm khởi tạo Gradle ở cấp root.", "settings.gradle.kts, app/build.gradle.kts")],
  ["settings.gradle.kts", note("Cấu hình gốc", "Đặt tên project BTLAND và include module app.", "Xác định cấu trúc module mà Gradle sẽ build.", "build.gradle.kts, app/build.gradle.kts")],
  ["gradle.properties", note("Cấu hình gốc", "Thiết lập JVM args, AndroidX và tối ưu build.", "Điều chỉnh hành vi Gradle trên máy phát triển.", "build.gradle.kts")],
  ["gradlew", note("Công cụ build", "Wrapper script cho Gradle trên Unix/Linux/macOS.", "Cho phép build mà không cần cài Gradle toàn cục.", "gradlew.bat, gradle/wrapper")],
  ["gradlew.bat", note("Công cụ build", "Wrapper script cho Gradle trên Windows.", "Lệnh chuẩn để build dự án trên máy Windows.", "gradlew, gradle/wrapper")],
  ["firestore.rules", note("Bảo mật Firebase", "Luật truy cập Firestore cho users, posts, conversations, messages và notifications.", "Bảo vệ dữ liệu chat, bài đăng và hồ sơ trên Firestore.", "storage.rules, app/src/google-services.json")],
  ["storage.rules", note("Bảo mật Firebase", "Luật truy cập Firebase Storage cho ảnh bài đăng và avatar.", "Kiểm soát quyền đọc/ghi ảnh trên Storage.", "firestore.rules, app/src/google-services.json")],
  ["FIREBASE_MAPS_SETUP.md", note("Tài liệu", "Tài liệu hướng dẫn cấu hình Firebase Storage, Google Maps, SHA1 và kiểm tra nhanh.", "Giúp đồng bộ console Firebase/Google Cloud với mã nguồn hiện tại.", "app/src/google-services.json, AndroidManifest.xml, firestore.rules, storage.rules")],
  ["app/.gitignore", note("Cấu hình module", "Danh sách file bỏ qua riêng cho module app.", "Tránh commit file build nội bộ của module.", ".gitignore")],
  ["app/build.gradle.kts", note("Cấu hình module", "Khai báo namespace, SDK, buildFeatures, dependency AndroidX, Firebase, Maps và Glide.", "Cấu hình build và thư viện cho module Android chính.", "build.gradle.kts, app/src/main/AndroidManifest.xml")],
  ["app/proguard-rules.pro", note("Cấu hình module", "Nơi định nghĩa rule ProGuard/R8 cho bản release.", "Phục vụ tối ưu/obfuscate khi cần build release.", "app/build.gradle.kts")],
  ["app/src/google-services.json", note("Cấu hình Firebase", "File cấu hình project Firebase, bucket Storage, app id và API key tích hợp.", "Liên kết ứng dụng Android với project Firebase thật.", "app/build.gradle.kts, firestore.rules, storage.rules")],
  ["app/src/main/AndroidManifest.xml", note("Manifest", "Khai báo permission, Application, activity và meta-data API key Google Maps.", "Điểm cấu hình hệ thống của app Android.", "app/build.gradle.kts, app/src/main/res/values/themes.xml")],

  ["app/src/main/java/com/example/btland/BTLApplication.java", note("Java/Application", "Application class của app.", "Khởi tạo theme đã lưu ngay khi app mở.", "ThemePreferences.java, MainActivity.java")],
  ["app/src/main/java/com/example/btland/activities/AdminUserManagementActivity.java", note("Java/Activity", "Màn hình quản trị người dùng.", "Cho admin xem danh sách user và thao tác khóa/mở khóa tài khoản.", "item_admin_user.xml, UserAdminAdapter.java, AdminFragment.java")],
  ["app/src/main/java/com/example/btland/activities/AdminUserPostsActivity.java", note("Java/Activity", "Màn hình quản trị bài đăng theo người dùng.", "Cho admin rà soát, ẩn hoặc xử lý bài đăng của user.", "item_admin_post.xml, AdminPostAdapter.java, AdminFragment.java")],
  ["app/src/main/java/com/example/btland/activities/ChatActivity.java", note("Java/Activity", "Màn hình chat 1-1 dùng Firestore.", "Tạo conversation, gửi message, cập nhật unread và đánh dấu đã đọc.", "Message.java, Conversation.java, MessageAdapter.java, firestore.rules")],
  ["app/src/main/java/com/example/btland/activities/CreatePostActivity.java", note("Java/Activity", "Màn hình tạo bài đăng mới.", "Nhập thông tin bài, chọn/chụp ảnh, lấy GPS, tạo ảnh 360 từ ảnh thường hoặc chọn panorama, rồi upload lên Firebase.", "activity_create_post.xml, Post.java, FirebaseStorageHelper.java")],
  ["app/src/main/java/com/example/btland/activities/EditPostActivity.java", note("Java/Activity", "Màn hình chỉnh sửa bài đăng hiện có.", "Cập nhật nội dung bài và geocode lại địa chỉ khi cần.", "activity_edit_post.xml, Post.java")],
  ["app/src/main/java/com/example/btland/activities/EditProfileActivity.java", note("Java/Activity", "Màn hình chỉnh sửa hồ sơ cá nhân.", "Cập nhật tên, số điện thoại và avatar lên Firebase.", "activity_edit_profile.xml, FirebaseStorageHelper.java, User.java")],
  ["app/src/main/java/com/example/btland/activities/LoginActivity.java", note("Java/Activity", "Màn hình đăng nhập.", "Xác thực người dùng và điều hướng vào app.", "activity_login.xml, WelcomeActivity.java, MainActivity.java")],
  ["app/src/main/java/com/example/btland/activities/MainActivity.java", note("Java/Activity", "Activity container chính của ứng dụng.", "Chứa BottomNavigation, chuyển fragment, giữ tab đang chọn và áp auto theme theo cảm biến sáng.", "activity_main.xml, HomeFragment.java, MapFragment.java, MessagesFragment.java, ProfileFragment.java")],
  ["app/src/main/java/com/example/btland/activities/MyPostsActivity.java", note("Java/Activity", "Màn hình danh sách bài của tôi.", "Hiển thị và quản lý các bài đăng thuộc user hiện tại.", "item_post.xml, PostRepository.java, PostAdapter.java")],
  ["app/src/main/java/com/example/btland/activities/PanoramaViewActivity.java", note("Java/Activity", "Màn hình xem ảnh panorama/360.", "Hiển thị ảnh 360 theo kiểu cuộn ngang và hỗ trợ gyroscope nếu thiết bị có.", "activity_panorama_view.xml, PostDetailActivity.java")],
  ["app/src/main/java/com/example/btland/activities/PostDetailActivity.java", note("Java/Activity", "Màn hình chi tiết bài đăng.", "Xem thông tin đầy đủ, ảnh, panorama và liên hệ chủ bài qua chat.", "activity_post_detail.xml, ChatActivity.java, PanoramaViewActivity.java")],
  ["app/src/main/java/com/example/btland/activities/RegisterActivity.java", note("Java/Activity", "Màn hình đăng ký tài khoản mới.", "Tạo user Firebase Auth và hồ sơ user trên Firestore.", "activity_register.xml, User.java")],
  ["app/src/main/java/com/example/btland/activities/WelcomeActivity.java", note("Java/Activity", "Màn hình chào đầu tiên.", "Điểm vào điều hướng sang đăng nhập hoặc đăng ký.", "activity_welcome.xml, LoginActivity.java, RegisterActivity.java")],

  ["app/src/main/java/com/example/btland/adapters/AdminPostAdapter.java", note("Java/Adapter", "Adapter hiển thị danh sách bài cho màn quản trị.", "Bind dữ liệu bài đăng vào item admin để kiểm duyệt/xử lý.", "item_admin_post.xml, Post.java")],
  ["app/src/main/java/com/example/btland/adapters/ConversationAdapter.java", note("Java/Adapter", "Adapter hiển thị danh sách hội thoại.", "Bind tên người chat, tin nhắn cuối, thời gian và badge unread.", "item_conversation.xml, Conversation.java, ChatActivity.java")],
  ["app/src/main/java/com/example/btland/adapters/MediaPreviewAdapter.java", note("Java/Adapter", "Adapter preview ảnh ngang.", "Hiển thị ảnh đã chọn hoặc ảnh xem trước khi tạo bài.", "item_media_preview.xml, CreatePostActivity.java")],
  ["app/src/main/java/com/example/btland/adapters/MessageAdapter.java", note("Java/Adapter", "Adapter hiển thị bubble tin nhắn.", "Phân biệt tin gửi/tin nhận, trạng thái đã đọc và nền bubble.", "item_message.xml, Message.java, ChatActivity.java")],
  ["app/src/main/java/com/example/btland/adapters/PostAdapter.java", note("Java/Adapter", "Adapter hiển thị danh sách bài đăng cho người dùng.", "Bind ảnh, giá, địa chỉ, loại phòng và trạng thái bài.", "item_post.xml, Post.java, PostDetailActivity.java")],
  ["app/src/main/java/com/example/btland/adapters/UserAdminAdapter.java", note("Java/Adapter", "Adapter hiển thị user ở màn admin.", "Bind thông tin người dùng và thao tác quản trị liên quan.", "item_admin_user.xml, User.java")],

  ["app/src/main/java/com/example/btland/fragments/AdminFragment.java", note("Java/Fragment", "Fragment tổng hợp công cụ quản trị.", "Điểm truy cập các màn quản lý user và bài đăng cho admin.", "fragment_admin.xml, AdminUserManagementActivity.java, AdminUserPostsActivity.java")],
  ["app/src/main/java/com/example/btland/fragments/HomeFragment.java", note("Java/Fragment", "Fragment trang chủ bài đăng.", "Tìm kiếm, mở bộ lọc ẩn/hiện, lọc bài và sắp xếp theo nhiều tiêu chí.", "fragment_home.xml, PostAdapter.java, Post.java")],
  ["app/src/main/java/com/example/btland/fragments/MapFragment.java", note("Java/Fragment", "Fragment bản đồ Google Maps.", "Hiển thị bài có tọa độ, ưu tiên bài gần người dùng và làm mờ vị trí bài ở ghép.", "fragment_map.xml, Post.java, PostDetailActivity.java")],
  ["app/src/main/java/com/example/btland/fragments/MessagesFragment.java", note("Java/Fragment", "Fragment danh sách hội thoại.", "Nghe thay đổi conversation realtime và mở ChatActivity.", "fragment_messages.xml, ConversationAdapter.java")],
  ["app/src/main/java/com/example/btland/fragments/ProfileFragment.java", note("Java/Fragment", "Fragment hồ sơ cá nhân.", "Hiển thị thông tin user, điều khiển theme, mở sửa hồ sơ, đăng tin và đăng xuất.", "fragment_profile.xml, EditProfileActivity.java, ThemePreferences.java")],

  ["app/src/main/java/com/example/btland/models/Conversation.java", note("Java/Model", "Model dữ liệu cho conversation.", "Lưu id hội thoại, userIds, lastMessage, lastTimestamp, unreadCounts và participantNames.", "ChatActivity.java, MessagesFragment.java")],
  ["app/src/main/java/com/example/btland/models/Message.java", note("Java/Model", "Model dữ liệu cho message.", "Lưu sender, receiver, nội dung, thời gian và trạng thái read.", "ChatActivity.java, MessageAdapter.java")],
  ["app/src/main/java/com/example/btland/models/Post.java", note("Java/Model", "Model dữ liệu bài đăng.", "Chứa thông tin mô tả bài, giá, diện tích, GPS, ảnh, panorama và trạng thái hiển thị.", "CreatePostActivity.java, PostAdapter.java, MapFragment.java")],
  ["app/src/main/java/com/example/btland/models/User.java", note("Java/Model", "Model dữ liệu người dùng.", "Chứa thông tin hồ sơ, vai trò admin, trạng thái khóa và avatar.", "RegisterActivity.java, EditProfileActivity.java, AdminUserManagementActivity.java")],

  ["app/src/main/java/com/example/btland/utils/FirebaseStorageHelper.java", note("Java/Utility", "Tiện ích thao tác Firebase Storage.", "Upload file, xóa thư mục và diễn giải lỗi Storage theo ngữ cảnh dễ hiểu.", "CreatePostActivity.java, EditProfileActivity.java, storage.rules")],
  ["app/src/main/java/com/example/btland/utils/PostRepository.java", note("Java/Utility", "Lớp tiện ích thao tác bài đăng.", "Đóng gói các thao tác xóa hoặc cập nhật bài để tái sử dụng.", "MyPostsActivity.java, Post.java, FirebaseStorageHelper.java")],
  ["app/src/main/java/com/example/btland/utils/ThemePreferences.java", note("Java/Utility", "Tiện ích lưu thiết lập theme.", "Lưu mode sáng/tối, auto theme và mode đã áp lần cuối bằng SharedPreferences.", "BTLApplication.java, MainActivity.java, ProfileFragment.java")],

  ["app/src/main/res/layout/activity_chat.xml", note("Resource/Layout", "Layout màn chat.", "Sắp xếp tiêu đề hội thoại, danh sách tin nhắn và ô nhập tin.", "ChatActivity.java")],
  ["app/src/main/res/layout/activity_create_post.xml", note("Resource/Layout", "Layout màn tạo bài đăng.", "Chứa form nhập bài, khu chọn ảnh, nút tạo panorama và preview bài.", "CreatePostActivity.java")],
  ["app/src/main/res/layout/activity_edit_post.xml", note("Resource/Layout", "Layout màn sửa bài đăng.", "Hiển thị form chỉnh sửa nội dung bài hiện có.", "EditPostActivity.java")],
  ["app/src/main/res/layout/activity_edit_profile.xml", note("Resource/Layout", "Layout màn sửa hồ sơ.", "Hiển thị avatar, tên, số điện thoại và nút lưu hồ sơ.", "EditProfileActivity.java")],
  ["app/src/main/res/layout/activity_login.xml", note("Resource/Layout", "Layout màn đăng nhập.", "Hiển thị ô email/mật khẩu và nút đăng nhập.", "LoginActivity.java")],
  ["app/src/main/res/layout/activity_main.xml", note("Resource/Layout", "Layout khung chính của app.", "Chứa FrameLayout và BottomNavigationView.", "MainActivity.java, bottom_nav_menu.xml")],
  ["app/src/main/res/layout/activity_my_posts.xml", note("Resource/Layout", "Layout màn bài đăng của tôi.", "Hiển thị danh sách bài thuộc user hiện tại.", "MyPostsActivity.java, PostAdapter.java")],
  ["app/src/main/res/layout/activity_panorama_view.xml", note("Resource/Layout", "Layout màn xem panorama.", "Chứa vùng cuộn ngang và hint điều khiển ảnh 360.", "PanoramaViewActivity.java")],
  ["app/src/main/res/layout/activity_post_detail.xml", note("Resource/Layout", "Layout màn chi tiết bài đăng.", "Hiển thị thông tin bài, ảnh, nút chat và panorama.", "PostDetailActivity.java")],
  ["app/src/main/res/layout/activity_register.xml", note("Resource/Layout", "Layout màn đăng ký.", "Hiển thị form tạo tài khoản mới.", "RegisterActivity.java")],
  ["app/src/main/res/layout/activity_welcome.xml", note("Resource/Layout", "Layout màn chào.", "Hiển thị lựa chọn đi tới đăng nhập hoặc đăng ký.", "WelcomeActivity.java")],
  ["app/src/main/res/layout/fragment_admin.xml", note("Resource/Layout", "Layout fragment quản trị.", "Bố trí các nút hoặc thẻ điều hướng cho admin.", "AdminFragment.java")],
  ["app/src/main/res/layout/fragment_home.xml", note("Resource/Layout", "Layout fragment trang chủ.", "Chứa thanh tìm kiếm, filter panel cuộn riêng và danh sách bài đăng.", "HomeFragment.java")],
  ["app/src/main/res/layout/fragment_map.xml", note("Resource/Layout", "Layout fragment bản đồ.", "Chứa MapView, trạng thái bản đồ và nút lấy lại GPS.", "MapFragment.java")],
  ["app/src/main/res/layout/fragment_messages.xml", note("Resource/Layout", "Layout fragment hội thoại.", "Hiển thị danh sách conversation và trạng thái rỗng.", "MessagesFragment.java")],
  ["app/src/main/res/layout/fragment_profile.xml", note("Resource/Layout", "Layout fragment hồ sơ.", "Hiển thị thông tin user, điều khiển theme và các nút thao tác cá nhân.", "ProfileFragment.java")],
  ["app/src/main/res/layout/item_admin_post.xml", note("Resource/Layout", "Layout item bài đăng cho admin.", "Card con dùng trong danh sách kiểm duyệt bài.", "AdminPostAdapter.java")],
  ["app/src/main/res/layout/item_admin_user.xml", note("Resource/Layout", "Layout item user cho admin.", "Card con dùng trong danh sách quản lý người dùng.", "UserAdminAdapter.java")],
  ["app/src/main/res/layout/item_conversation.xml", note("Resource/Layout", "Layout item hội thoại.", "Card con hiển thị người chat, tin cuối và badge unread.", "ConversationAdapter.java")],
  ["app/src/main/res/layout/item_media_preview.xml", note("Resource/Layout", "Layout item preview media.", "Item ảnh nhỏ cho danh sách preview ngang.", "MediaPreviewAdapter.java")],
  ["app/src/main/res/layout/item_message.xml", note("Resource/Layout", "Layout item tin nhắn.", "Khung bubble tin nhắn cho message gửi/nhận.", "MessageAdapter.java")],
  ["app/src/main/res/layout/item_post.xml", note("Resource/Layout", "Layout item bài đăng.", "Card bài đăng hiển thị ảnh đại diện và thông tin tóm tắt.", "PostAdapter.java")],
  ["app/src/main/res/menu/bottom_nav_menu.xml", note("Resource/Menu", "Menu Bottom Navigation.", "Định nghĩa các tab Trang chủ, Bản đồ, Tin nhắn, Cá nhân và Quản trị.", "activity_main.xml, MainActivity.java")],
  ["app/src/main/res/values/colors.xml", note("Resource/Value", "Bảng màu dùng toàn app.", "Tập trung các mã màu chủ đạo cho UI.", "themes.xml, drawable/bg_*.xml")],
  ["app/src/main/res/values/strings.xml", note("Resource/Value", "Kho chuỗi văn bản tổng quát của Android.", "Nơi nên đặt string dùng lại nếu cần chuẩn hóa thêm.", "layout/*.xml")],
  ["app/src/main/res/values/themes.xml", note("Resource/Theme", "Theme sáng và mặc định của ứng dụng.", "Quy định màu chủ đạo, status bar, navigation bar và window background.", "themes-night/themes.xml, MainActivity.java")],
  ["app/src/main/res/values-night/themes.xml", note("Resource/Theme", "Theme tối của ứng dụng.", "Tùy biến giao diện khi app chuyển sang dark mode.", "values/themes.xml, ThemePreferences.java")],
  ["app/src/main/res/xml/backup_rules.xml", note("Resource/XML", "Cấu hình auto backup Android.", "Điều khiển phạm vi dữ liệu được backup/restore.", "AndroidManifest.xml")],
  ["app/src/main/res/xml/data_extraction_rules.xml", note("Resource/XML", "Cấu hình data extraction cho Android 12+.", "Kiểm soát dữ liệu cho backup và transfer.", "AndroidManifest.xml")],
]);

function note(group, contains, purpose, related) {
  return { group, contains, purpose, related };
}

function toRel(filePath) {
  return filePath.replaceAll("\\", "/");
}

async function walk(dir) {
  const entries = await fs.readdir(path.join(repoRoot, dir), { withFileTypes: true });
  const out = [];
  for (const entry of entries) {
    const rel = `${dir}/${entry.name}`.replaceAll("\\", "/");
    if (entry.isDirectory()) {
      out.push(...await walk(rel));
    } else {
      out.push(rel);
    }
  }
  return out;
}

function baseName(rel) {
  return path.basename(rel, path.extname(rel));
}

function humanizeSnake(text) {
  return text
    .replace(/^activity_/, "")
    .replace(/^fragment_/, "")
    .replace(/^item_/, "")
    .replace(/^bg_/, "")
    .replace(/^ic_/, "")
    .split("_")
    .filter(Boolean)
    .map(word => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ");
}

function humanizeCamel(text) {
  return text
    .replace(/Activity$|Fragment$|Adapter$|Helper$|Repository$|Preferences$|View$/g, "")
    .replace(/([a-z])([A-Z])/g, "$1 $2")
    .trim();
}

function relatedForLayout(rel) {
  const name = baseName(rel);
  if (name.startsWith("activity_")) {
    return `${humanizeSnake(name)}Activity.java`;
  }
  if (name.startsWith("fragment_")) {
    return `${humanizeSnake(name)}Fragment.java`;
  }
  if (name.startsWith("item_")) {
    return "Adapter tương ứng trong thư mục adapters/";
  }
  return "";
}

function describeGeneric(rel) {
  const file = baseName(rel);

  if (rel.startsWith("app/src/main/java/com/example/btland/activities/")) {
    const screen = humanizeCamel(file);
    return note(
      "Java/Activity",
      `Lớp Activity cho màn hình ${screen}.`,
      `Điều phối sự kiện UI, dữ liệu Firebase và điều hướng liên quan tới màn ${screen}.`,
      `activity_${screen.toLowerCase().replaceAll(" ", "_")}.xml`
    );
  }

  if (rel.startsWith("app/src/main/java/com/example/btland/fragments/")) {
    const screen = humanizeCamel(file);
    return note(
      "Java/Fragment",
      `Lớp Fragment cho khu vực ${screen}.`,
      `Tạo phần giao diện con gắn trong MainActivity và quản lý logic của tab ${screen}.`,
      `fragment_${screen.toLowerCase().replaceAll(" ", "_")}.xml`
    );
  }

  if (rel.startsWith("app/src/main/java/com/example/btland/adapters/")) {
    const screen = humanizeCamel(file);
    return note(
      "Java/Adapter",
      `Adapter RecyclerView cho ${screen}.`,
      `Chuyển dữ liệu model thành item hiển thị trong danh sách tương ứng.`,
      "item_*.xml"
    );
  }

  if (rel.startsWith("app/src/main/java/com/example/btland/models/")) {
    const screen = humanizeCamel(file);
    return note(
      "Java/Model",
      `Model dữ liệu ${screen}.`,
      `Đóng vai trò cấu trúc dữ liệu trao đổi với Firestore hoặc UI.`,
      "activities/, fragments/, adapters/"
    );
  }

  if (rel.startsWith("app/src/main/java/com/example/btland/utils/")) {
    const screen = humanizeCamel(file);
    return note(
      "Java/Utility",
      `Lớp tiện ích ${screen}.`,
      `Tách logic dùng chung ra khỏi Activity/Fragment để dễ tái sử dụng.`,
      "activities/, fragments/"
    );
  }

  if (rel.startsWith("app/src/main/res/layout/")) {
    if (file.startsWith("activity_")) {
      const name = humanizeSnake(file);
      return note("Resource/Layout", `Bố cục XML cho màn hình ${name}.`, `Định nghĩa cây view của màn ${name}.`, relatedForLayout(rel));
    }
    if (file.startsWith("fragment_")) {
      const name = humanizeSnake(file);
      return note("Resource/Layout", `Bố cục XML cho fragment ${name}.`, `Định nghĩa phần giao diện dùng trong tab hoặc vùng nội dung ${name}.`, relatedForLayout(rel));
    }
    if (file.startsWith("item_")) {
      const name = humanizeSnake(file);
      return note("Resource/Layout", `Bố cục item ${name}.`, `Định nghĩa card/dòng con dùng cho RecyclerView hoặc preview.`, relatedForLayout(rel));
    }
  }

  if (rel.startsWith("app/src/main/res/drawable/")) {
    if (file.startsWith("bg_")) {
      return note("Resource/Drawable", `Shape/background ${humanizeSnake(file)}.`, "Cung cấp nền, bo góc, viền hoặc badge cho UI.", "layout/*.xml");
    }
    if (file.startsWith("ic_")) {
      return note("Resource/Drawable", `Icon vector ${humanizeSnake(file)}.`, "Cung cấp icon hiển thị cho nút, nav hoặc trạng thái.", "layout/*.xml, menu/*.xml");
    }
    return note("Resource/Drawable", `Tài nguyên drawable ${humanizeSnake(file)}.`, "Phục vụ hiển thị đồ họa trong giao diện.", "layout/*.xml");
  }

  if (rel.startsWith("app/src/main/res/color/")) {
    return note("Resource/Color", `Color selector ${humanizeSnake(file)}.`, "Điều khiển màu động theo trạng thái selected/checked.", "activity_main.xml, menu/*.xml");
  }

  if (rel.startsWith("app/src/main/res/mipmap-")) {
    return note("Launcher Asset", `Launcher icon density ${path.basename(path.dirname(rel))}.`, "Biến thể icon ứng dụng theo từng mật độ màn hình.", "AndroidManifest.xml");
  }

  if (rel.startsWith("app/src/main/res/mipmap-anydpi-v26/")) {
    return note("Launcher Asset", `Adaptive icon ${file}.`, "Định nghĩa adaptive launcher icon cho Android mới.", "ic_launcher_background.xml, ic_launcher_foreground.xml");
  }

  return note("Khác", `Tệp ${file}.`, "Tài nguyên hoặc cấu hình hỗ trợ cho dự án.", "");
}

function describeFile(rel) {
  if (explicit.has(rel)) {
    return explicit.get(rel);
  }
  return describeGeneric(rel);
}

async function collectFiles() {
  const rels = new Set(fixedFiles);
  for (const dir of [
    "app/src/main/java/com/example/btland",
    "app/src/main/res",
  ]) {
    for (const rel of await walk(dir)) {
      rels.add(rel);
    }
  }
  return Array.from(rels).sort((a, b) => a.localeCompare(b));
}

function buildRows(files) {
  return files.map((rel, index) => {
    const info = describeFile(rel);
    return [
      index + 1,
      info.group,
      rel,
      info.contains,
      info.purpose,
      info.related,
    ];
  });
}

function countByGroup(rows) {
  const map = new Map();
  for (const row of rows) {
    const group = row[1];
    map.set(group, (map.get(group) ?? 0) + 1);
  }
  return Array.from(map.entries())
    .sort((a, b) => a[0].localeCompare(b[0]))
    .map(([group, count]) => [group, count]);
}

function applyHeaderStyle(range, fillColor) {
  range.format = {
    fill: fillColor,
    font: { bold: true, color: "#FFFFFF" },
    horizontalAlignment: "center",
    verticalAlignment: "center",
  };
}

async function saveBlob(blob, targetPath) {
  const bytes = new Uint8Array(await blob.arrayBuffer());
  await fs.writeFile(targetPath, bytes);
}

const files = await collectFiles();
const rows = buildRows(files);
const counts = countByGroup(rows);

const workbook = Workbook.create();
const summary = workbook.worksheets.add("Tong quan");
const detail = workbook.worksheets.add("Danh muc file");
const notes = workbook.worksheets.add("Ghi chu");

summary.getRange("A1:D1").merge();
summary.getRange("A1").values = [["TÀI LIỆU CẤU TRÚC DỰ ÁN BTLAND"]];
summary.getRange("A1:D1").format = {
  fill: "#0F766E",
  font: { bold: true, color: "#FFFFFF", size: 16 },
  horizontalAlignment: "center",
  verticalAlignment: "center",
};
summary.getRange("A3:B6").values = [
  ["Mục", "Giá trị"],
  ["Tên project", "BTLAND"],
  ["Tổng số file được mô tả", rows.length],
  ["Phạm vi", "Root config + app/src/main/java + app/src/main/res + Firebase rules"],
];
applyHeaderStyle(summary.getRange("A3:B3"), "#2563EB");
summary.getRange("A8:C8").values = [["Nhóm file", "Số lượng", "Ghi chú"]];
applyHeaderStyle(summary.getRange("A8:C8"), "#7C3AED");
summary.getRange(`A9:C${8 + counts.length}`).values = counts.map(([group, count]) => [
  group,
  count,
  `Số file thuộc nhóm ${group}`,
]);
summary.getRange("A3:C30").format.wrapText = true;
summary.getRange("A1:D30").format.verticalAlignment = "center";
summary.getRange("A1:D30").format.autofitColumns();
summary.freezePanes.freezeRows(1);
summary.showGridLines = false;

const detailHeader = [["STT", "Nhóm", "Đường dẫn file", "File chứa gì", "Chức năng chính", "Liên quan trực tiếp"]];
detail.getRange("A1:F1").values = detailHeader;
applyHeaderStyle(detail.getRange("A1:F1"), "#1D4ED8");
detail.getRange(`A2:F${rows.length + 1}`).values = rows;
detail.freezePanes.freezeRows(1);
detail.showGridLines = false;
detail.getRange(`A1:F${rows.length + 1}`).format.wrapText = true;
detail.getRange(`A1:F${rows.length + 1}`).format.verticalAlignment = "top";
detail.getRange(`A1:A${rows.length + 1}`).format.horizontalAlignment = "center";
detail.getRange(`A1:F${rows.length + 1}`).format.autofitRows();
detail.getRange(`A1:A${rows.length + 1}`).format.columnWidthPx = 60;
detail.getRange(`B1:B${rows.length + 1}`).format.columnWidthPx = 150;
detail.getRange(`C1:C${rows.length + 1}`).format.columnWidthPx = 360;
detail.getRange(`D1:D${rows.length + 1}`).format.columnWidthPx = 280;
detail.getRange(`E1:E${rows.length + 1}`).format.columnWidthPx = 320;
detail.getRange(`F1:F${rows.length + 1}`).format.columnWidthPx = 240;

notes.getRange("A1:D1").merge();
notes.getRange("A1").values = [["GHI CHÚ KHI ĐỌC TÀI LIỆU"]];
notes.getRange("A1:D1").format = {
  fill: "#EA580C",
  font: { bold: true, color: "#FFFFFF", size: 14 },
  horizontalAlignment: "center",
};
notes.getRange("A3:B8").values = [
  ["Mục", "Diễn giải"],
  ["Java/Activity", "Các màn hình độc lập của ứng dụng Android."],
  ["Java/Fragment", "Các tab hoặc vùng nội dung gắn trong MainActivity."],
  ["Java/Adapter", "Cầu nối dữ liệu -> item RecyclerView/List."],
  ["Java/Model", "Cấu trúc dữ liệu làm việc với Firestore/UI."],
  ["Java/Utility", "Hàm/lớp hỗ trợ dùng chung như Storage, theme, repository."],
];
applyHeaderStyle(notes.getRange("A3:B3"), "#0F766E");
notes.getRange("A10:B14").values = [
  ["Lưu ý 1", "Chat hiện dùng Firestore, không dùng Firebase Storage."],
  ["Lưu ý 2", "Maps và Firebase cần publish đúng rules/cấu hình console để chạy trên máy thật."],
  ["Lưu ý 3", "Ảnh 360 hiện có thể tạo nhanh từ ảnh đầu tiên ngay trong app."],
  ["Lưu ý 4", "Danh mục này mô tả file quan trọng phục vụ build và chạy app, không liệt kê thư mục build sinh tự động."],
  ["Lưu ý 5", "Các file icon launcher khác nhau chủ yếu khác mật độ màn hình nhưng cùng chức năng hiển thị biểu tượng app."],
];
notes.getRange("A1:B20").format.wrapText = true;
notes.getRange("A1:B20").format.autofitColumns();
notes.showGridLines = false;

await fs.mkdir(outputDir, { recursive: true });

const summaryPreview = await workbook.render({
  sheetName: "Tong quan",
  range: "A1:D20",
  scale: 1.4,
  format: "png",
});
await saveBlob(summaryPreview, summaryPreviewPath);

const detailPreview = await workbook.render({
  sheetName: "Danh muc file",
  range: "A1:F20",
  scale: 1.2,
  format: "png",
});
await saveBlob(detailPreview, detailPreviewPath);

const inspectSummary = await workbook.inspect({
  kind: "table",
  range: "Tong quan!A1:D15",
  include: "values,formulas",
  tableMaxRows: 15,
  tableMaxCols: 4,
});
console.log(inspectSummary.ndjson);

const inspectDetail = await workbook.inspect({
  kind: "table",
  range: "Danh muc file!A1:F12",
  include: "values,formulas",
  tableMaxRows: 12,
  tableMaxCols: 6,
});
console.log(inspectDetail.ndjson);

const output = await SpreadsheetFile.exportXlsx(workbook);
await output.save(workbookPath);

console.log(JSON.stringify({
  workbookPath,
  summaryPreviewPath,
  detailPreviewPath,
  fileCount: rows.length,
}, null, 2));
