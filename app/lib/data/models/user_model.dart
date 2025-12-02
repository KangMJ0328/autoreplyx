class UserModel {
  final int id;
  final String email;
  final String brandName;
  final String? industry;
  final String? description;
  final String? businessHours;
  final String? address;
  final String? phone;
  final String? profileImage;
  final String? aiTone;
  final String? bannedWords;
  final String plan;
  final int messageLimit;
  final int messageCount;
  final String? planExpiresAt;
  final bool isActive;
  final String createdAt;
  final String? updatedAt;

  UserModel({
    required this.id,
    required this.email,
    required this.brandName,
    this.industry,
    this.description,
    this.businessHours,
    this.address,
    this.phone,
    this.profileImage,
    this.aiTone,
    this.bannedWords,
    required this.plan,
    required this.messageLimit,
    required this.messageCount,
    this.planExpiresAt,
    required this.isActive,
    required this.createdAt,
    this.updatedAt,
  });

  factory UserModel.fromJson(Map<String, dynamic> json) {
    return UserModel(
      id: json['id'] as int,
      email: json['email'] as String,
      brandName: json['brandName'] as String,
      industry: json['industry'] as String?,
      description: json['description'] as String?,
      businessHours: json['businessHours'] as String?,
      address: json['address'] as String?,
      phone: json['phone'] as String?,
      profileImage: json['profileImage'] as String?,
      aiTone: json['aiTone'] as String?,
      bannedWords: json['bannedWords'] as String?,
      plan: json['plan'] as String? ?? 'free',
      messageLimit: json['messageLimit'] as int? ?? 100,
      messageCount: json['messageCount'] as int? ?? 0,
      planExpiresAt: json['planExpiresAt'] as String?,
      isActive: json['isActive'] as bool? ?? true,
      createdAt: json['createdAt'] as String? ?? '',
      updatedAt: json['updatedAt'] as String?,
    );
  }

  Map<String, dynamic> toJson() => {
    'id': id,
    'email': email,
    'brandName': brandName,
    'industry': industry,
    'description': description,
    'businessHours': businessHours,
    'address': address,
    'phone': phone,
    'profileImage': profileImage,
    'aiTone': aiTone,
    'bannedWords': bannedWords,
    'plan': plan,
    'messageLimit': messageLimit,
    'messageCount': messageCount,
    'planExpiresAt': planExpiresAt,
    'isActive': isActive,
    'createdAt': createdAt,
    'updatedAt': updatedAt,
  };

  UserModel copyWith({
    int? id,
    String? email,
    String? brandName,
    String? industry,
    String? description,
    String? businessHours,
    String? address,
    String? phone,
    String? profileImage,
    String? aiTone,
    String? bannedWords,
    String? plan,
    int? messageLimit,
    int? messageCount,
    String? planExpiresAt,
    bool? isActive,
    String? createdAt,
    String? updatedAt,
  }) {
    return UserModel(
      id: id ?? this.id,
      email: email ?? this.email,
      brandName: brandName ?? this.brandName,
      industry: industry ?? this.industry,
      description: description ?? this.description,
      businessHours: businessHours ?? this.businessHours,
      address: address ?? this.address,
      phone: phone ?? this.phone,
      profileImage: profileImage ?? this.profileImage,
      aiTone: aiTone ?? this.aiTone,
      bannedWords: bannedWords ?? this.bannedWords,
      plan: plan ?? this.plan,
      messageLimit: messageLimit ?? this.messageLimit,
      messageCount: messageCount ?? this.messageCount,
      planExpiresAt: planExpiresAt ?? this.planExpiresAt,
      isActive: isActive ?? this.isActive,
      createdAt: createdAt ?? this.createdAt,
      updatedAt: updatedAt ?? this.updatedAt,
    );
  }
}
