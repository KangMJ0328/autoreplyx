class ApiConstants {
  static const String baseUrl = 'http://localhost:8080/api';

  // Auth endpoints
  static const String login = '/auth/login';
  static const String register = '/auth/register';
  static const String refreshToken = '/auth/refresh';
  static const String logout = '/auth/logout';

  // User endpoints
  static const String profile = '/users/me';
  static const String updateProfile = '/users/me';
  static const String updatePassword = '/users/me/password';

  // Channel endpoints
  static const String channels = '/channels';
  static const String instagramAuth = '/channels/instagram/auth-url';
  static const String instagramCallback = '/channels/instagram/callback';
  static const String instagramWebhook = '/channels/instagram/webhook';

  // Rule endpoints
  static const String rules = '/rules';

  // Message endpoints
  static const String messages = '/messages';
  static const String messageLogs = '/messages/logs';

  // Subscription endpoints
  static const String subscription = '/subscription';
  static const String subscriptionPlans = '/subscription/plans';
  static const String subscriptionPayment = '/subscription/payment';

  // Dashboard endpoints
  static const String dashboardStats = '/dashboard/stats';
  static const String dashboardChart = '/dashboard/chart';

  // Settings endpoints
  static const String settings = '/settings';
  static const String businessInfo = '/settings/business';
  static const String aiSettings = '/settings/ai';
}
