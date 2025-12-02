class ChannelModel {
  final int id;
  final String platform;
  final String? platformUserId;
  final String? username;
  final String? accessToken;
  final String? refreshToken;
  final String? tokenExpiresAt;
  final bool isActive;
  final String? profileUrl;
  final String createdAt;
  final String? updatedAt;

  ChannelModel({
    required this.id,
    required this.platform,
    this.platformUserId,
    this.username,
    this.accessToken,
    this.refreshToken,
    this.tokenExpiresAt,
    required this.isActive,
    this.profileUrl,
    required this.createdAt,
    this.updatedAt,
  });

  factory ChannelModel.fromJson(Map<String, dynamic> json) {
    return ChannelModel(
      id: json['id'] as int,
      platform: json['platform'] as String,
      platformUserId: json['platformUserId'] as String?,
      username: json['username'] as String?,
      accessToken: json['accessToken'] as String?,
      refreshToken: json['refreshToken'] as String?,
      tokenExpiresAt: json['tokenExpiresAt'] as String?,
      isActive: json['isActive'] as bool? ?? true,
      profileUrl: json['profileUrl'] as String?,
      createdAt: json['createdAt'] as String? ?? '',
      updatedAt: json['updatedAt'] as String?,
    );
  }

  Map<String, dynamic> toJson() => {
    'id': id,
    'platform': platform,
    'platformUserId': platformUserId,
    'username': username,
    'accessToken': accessToken,
    'refreshToken': refreshToken,
    'tokenExpiresAt': tokenExpiresAt,
    'isActive': isActive,
    'profileUrl': profileUrl,
    'createdAt': createdAt,
    'updatedAt': updatedAt,
  };

  bool get isConnected => accessToken != null && accessToken!.isNotEmpty;

  ChannelModel copyWith({
    int? id,
    String? platform,
    String? platformUserId,
    String? username,
    String? accessToken,
    String? refreshToken,
    String? tokenExpiresAt,
    bool? isActive,
    String? profileUrl,
    String? createdAt,
    String? updatedAt,
  }) {
    return ChannelModel(
      id: id ?? this.id,
      platform: platform ?? this.platform,
      platformUserId: platformUserId ?? this.platformUserId,
      username: username ?? this.username,
      accessToken: accessToken ?? this.accessToken,
      refreshToken: refreshToken ?? this.refreshToken,
      tokenExpiresAt: tokenExpiresAt ?? this.tokenExpiresAt,
      isActive: isActive ?? this.isActive,
      profileUrl: profileUrl ?? this.profileUrl,
      createdAt: createdAt ?? this.createdAt,
      updatedAt: updatedAt ?? this.updatedAt,
    );
  }
}

class InstagramAuthUrl {
  final String authUrl;

  InstagramAuthUrl({required this.authUrl});

  factory InstagramAuthUrl.fromJson(Map<String, dynamic> json) {
    return InstagramAuthUrl(authUrl: json['authUrl'] as String);
  }
}
