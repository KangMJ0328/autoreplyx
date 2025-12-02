class MessageLogModel {
  final int id;
  final String platform;
  final String messageType;
  final String senderType;
  final String? senderId;
  final String? senderName;
  final String content;
  final String? response;
  final String status;
  final int? tokensUsed;
  final String? errorMessage;
  final String createdAt;
  final String? processedAt;

  MessageLogModel({
    required this.id,
    required this.platform,
    required this.messageType,
    required this.senderType,
    this.senderId,
    this.senderName,
    required this.content,
    this.response,
    required this.status,
    this.tokensUsed,
    this.errorMessage,
    required this.createdAt,
    this.processedAt,
  });

  factory MessageLogModel.fromJson(Map<String, dynamic> json) {
    return MessageLogModel(
      id: json['id'] as int,
      platform: json['platform'] as String,
      messageType: json['messageType'] as String? ?? 'dm',
      senderType: json['senderType'] as String? ?? 'user',
      senderId: json['senderId'] as String?,
      senderName: json['senderName'] as String?,
      content: json['content'] as String,
      response: json['response'] as String?,
      status: json['status'] as String,
      tokensUsed: json['tokensUsed'] as int?,
      errorMessage: json['errorMessage'] as String?,
      createdAt: json['createdAt'] as String? ?? '',
      processedAt: json['processedAt'] as String?,
    );
  }

  Map<String, dynamic> toJson() => {
    'id': id,
    'platform': platform,
    'messageType': messageType,
    'senderType': senderType,
    'senderId': senderId,
    'senderName': senderName,
    'content': content,
    'response': response,
    'status': status,
    'tokensUsed': tokensUsed,
    'errorMessage': errorMessage,
    'createdAt': createdAt,
    'processedAt': processedAt,
  };

  bool get isProcessed => status == 'completed';
  bool get isFailed => status == 'failed';
  bool get isPending => status == 'pending';
}

class MessageLogPage {
  final List<MessageLogModel> content;
  final int totalElements;
  final int totalPages;
  final int size;
  final int number;
  final bool first;
  final bool last;

  MessageLogPage({
    required this.content,
    required this.totalElements,
    required this.totalPages,
    required this.size,
    required this.number,
    required this.first,
    required this.last,
  });

  factory MessageLogPage.fromJson(Map<String, dynamic> json) {
    return MessageLogPage(
      content: (json['content'] as List)
          .map((e) => MessageLogModel.fromJson(e as Map<String, dynamic>))
          .toList(),
      totalElements: json['totalElements'] as int? ?? 0,
      totalPages: json['totalPages'] as int? ?? 0,
      size: json['size'] as int? ?? 20,
      number: json['number'] as int? ?? 0,
      first: json['first'] as bool? ?? true,
      last: json['last'] as bool? ?? true,
    );
  }
}
