class SubscriptionPlan {
  final String id;
  final String name;
  final String description;
  final int price;
  final int messageLimit;
  final List<String> features;
  final bool isPopular;

  SubscriptionPlan({
    required this.id,
    required this.name,
    required this.description,
    required this.price,
    required this.messageLimit,
    required this.features,
    this.isPopular = false,
  });

  factory SubscriptionPlan.fromJson(Map<String, dynamic> json) {
    return SubscriptionPlan(
      id: json['id'] as String,
      name: json['name'] as String,
      description: json['description'] as String? ?? '',
      price: json['price'] as int? ?? 0,
      messageLimit: json['messageLimit'] as int? ?? 100,
      features: (json['features'] as List?)?.map((e) => e as String).toList() ?? [],
      isPopular: json['isPopular'] as bool? ?? false,
    );
  }

  String get formattedPrice => price == 0 ? '무료' : '₩${price.toString().replaceAllMapped(RegExp(r'(\d)(?=(\d{3})+(?!\d))'), (m) => '${m[1]},')}';
  String get formattedMessageLimit => messageLimit == -1 ? '무제한' : '$messageLimit건';
}

class UserSubscription {
  final String plan;
  final int messageLimit;
  final int messageCount;
  final String? expiresAt;
  final String? subscribedAt;
  final bool isActive;
  final bool autoRenew;

  UserSubscription({
    required this.plan,
    required this.messageLimit,
    required this.messageCount,
    this.expiresAt,
    this.subscribedAt,
    required this.isActive,
    this.autoRenew = false,
  });

  factory UserSubscription.fromJson(Map<String, dynamic> json) {
    return UserSubscription(
      plan: json['plan'] as String? ?? 'free',
      messageLimit: json['messageLimit'] as int? ?? 100,
      messageCount: json['messageCount'] as int? ?? 0,
      expiresAt: json['expiresAt'] as String?,
      subscribedAt: json['subscribedAt'] as String?,
      isActive: json['isActive'] as bool? ?? true,
      autoRenew: json['autoRenew'] as bool? ?? false,
    );
  }

  int get remaining => messageLimit - messageCount;
  double get usagePercentage => messageLimit > 0 ? (messageCount / messageLimit * 100) : 0;
  bool get isNearLimit => usagePercentage >= 80;
}

class PaymentRequest {
  final String planId;
  final String paymentMethod;

  PaymentRequest({required this.planId, required this.paymentMethod});

  Map<String, dynamic> toJson() => {
    'planId': planId,
    'paymentMethod': paymentMethod,
  };
}
