class AppConstants {
  static const String appName = 'AutoReplyX';
  static const String appVersion = '1.0.0';

  // Storage keys
  static const String accessTokenKey = 'access_token';
  static const String refreshTokenKey = 'refresh_token';
  static const String userKey = 'user_data';
  static const String themeKey = 'theme_mode';
  static const String localeKey = 'locale';
  static const String onboardingKey = 'onboarding_completed';

  // Timeouts
  static const int connectionTimeout = 30000;
  static const int receiveTimeout = 30000;

  // Pagination
  static const int defaultPageSize = 20;

  // Cache durations
  static const int cacheDurationMinutes = 5;

  // Validation
  static const int minPasswordLength = 8;
  static const int maxPasswordLength = 50;
  static const int maxBrandNameLength = 100;

  // Plans
  static const List<String> planTypes = ['free', 'basic', 'premium', 'enterprise'];
}
