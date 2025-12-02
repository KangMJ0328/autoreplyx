import { useState } from 'react';
import { Calendar, User, Mail, Phone, Clock, FileText, CheckCircle, XCircle, Filter, Loader2, AlertCircle } from 'lucide-react';
import { Card, CardContent } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../components/ui/select';
import { useReservations } from '../hooks/useReservations';

export default function Reservations() {
  const [filter, setFilter] = useState('all');
  const { reservations, isLoading, error, updateStatus, addNote } = useReservations();

  const filteredReservations = reservations.filter((r) => {
    if (filter === 'all') return true;
    return r.status === filter;
  });

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'pending':
        return 'bg-yellow-50 text-yellow-700 border-yellow-200';
      case 'confirmed':
        return 'bg-green-50 text-green-700 border-green-200';
      case 'cancelled':
        return 'bg-red-50 text-red-700 border-red-200';
      case 'completed':
        return 'bg-blue-50 text-blue-700 border-blue-200';
      default:
        return 'bg-gray-50 text-gray-700 border-gray-200';
    }
  };

  const getStatusLabel = (status: string) => {
    switch (status) {
      case 'pending':
        return '대기';
      case 'confirmed':
        return '확정';
      case 'cancelled':
        return '취소';
      case 'completed':
        return '완료';
      default:
        return status;
    }
  };

  const handleConfirm = async (id: number) => {
    await updateStatus(id, 'confirmed');
  };

  const handleCancel = async (id: number) => {
    await updateStatus(id, 'cancelled');
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('ko-KR');
  };

  const formatTime = (timeString: string) => {
    return timeString.substring(0, 5);
  };

  if (isLoading) {
    return (
      <div className="p-8 flex items-center justify-center h-full">
        <div className="text-center">
          <Loader2 className="w-8 h-8 text-blue-500 animate-spin mx-auto mb-4" />
          <p className="text-gray-600">예약 정보를 불러오는 중...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-8 flex items-center justify-center h-full">
        <div className="text-center">
          <AlertCircle className="w-8 h-8 text-red-500 mx-auto mb-4" />
          <p className="text-red-600">{error}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="p-8">
      {/* Header */}
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-gray-900 mb-2">예약 관리</h1>
          <p className="text-gray-600">고객 예약을 확인하고 관리하세요</p>
        </div>
        <div className="flex items-center gap-3">
          <Filter className="w-5 h-5 text-gray-500" />
          <Select value={filter} onValueChange={setFilter}>
            <SelectTrigger className="w-40">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">전체</SelectItem>
              <SelectItem value="pending">대기</SelectItem>
              <SelectItem value="confirmed">확정</SelectItem>
              <SelectItem value="completed">완료</SelectItem>
              <SelectItem value="cancelled">취소</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      {/* Reservations List */}
      {filteredReservations.length > 0 ? (
        <div className="grid gap-4">
          {filteredReservations.map((reservation) => (
            <Card key={reservation.id}>
              <CardContent className="p-6">
                <div className="flex items-start justify-between mb-4">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-full bg-blue-50 flex items-center justify-center">
                      <User className="w-5 h-5 text-blue-600" />
                    </div>
                    <div>
                      <h3 className="text-gray-900 mb-1">{reservation.customer_name}</h3>
                      <Badge className={getStatusColor(reservation.status)}>
                        {getStatusLabel(reservation.status)}
                      </Badge>
                    </div>
                  </div>
                  {reservation.status === 'pending' && (
                    <div className="flex gap-2">
                      <Button
                        size="sm"
                        className="bg-green-600 hover:bg-green-700"
                        onClick={() => handleConfirm(reservation.id)}
                      >
                        <CheckCircle className="w-4 h-4 mr-1" />
                        확정
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleCancel(reservation.id)}
                      >
                        <XCircle className="w-4 h-4 mr-1" />
                        취소
                      </Button>
                    </div>
                  )}
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 text-sm">
                  <div className="flex items-center gap-2">
                    <Phone className="w-4 h-4 text-gray-500" />
                    <div>
                      <p className="text-xs text-gray-500">연락처</p>
                      <p className="text-gray-900">{reservation.customer_phone || '-'}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <Mail className="w-4 h-4 text-gray-500" />
                    <div>
                      <p className="text-xs text-gray-500">이메일</p>
                      <p className="text-gray-900">{reservation.customer_email || '-'}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <FileText className="w-4 h-4 text-gray-500" />
                    <div>
                      <p className="text-xs text-gray-500">서비스</p>
                      <p className="text-gray-900">{reservation.service_name || '-'}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <Calendar className="w-4 h-4 text-gray-500" />
                    <div>
                      <p className="text-xs text-gray-500">예약 날짜</p>
                      <p className="text-gray-900">{formatDate(reservation.reservation_date)}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <Clock className="w-4 h-4 text-gray-500" />
                    <div>
                      <p className="text-xs text-gray-500">예약 시간</p>
                      <p className="text-gray-900">{formatTime(reservation.reservation_time)}</p>
                    </div>
                  </div>
                </div>

                {reservation.customer_requests && (
                  <div className="mt-4 pt-4 border-t border-gray-200">
                    <p className="text-xs text-gray-500 mb-1">요청사항</p>
                    <p className="text-gray-900">{reservation.customer_requests}</p>
                  </div>
                )}
              </CardContent>
            </Card>
          ))}
        </div>
      ) : (
        <Card>
          <CardContent className="p-12 text-center">
            <div className="w-16 h-16 rounded-full bg-gray-100 flex items-center justify-center mx-auto mb-4">
              <Calendar className="w-8 h-8 text-gray-400" />
            </div>
            <h3 className="text-gray-900 mb-2">예약이 없습니다</h3>
            <p className="text-gray-600">현재 {filter === 'all' ? '등록된' : getStatusLabel(filter)} 예약이 없습니다.</p>
          </CardContent>
        </Card>
      )}

      {/* Guide Section */}
      <Card className="mt-6">
        <CardContent className="p-6">
          <h3 className="text-gray-900 mb-3">예약 관리 안내</h3>
          <ul className="space-y-2 text-gray-600">
            <li className="flex items-start gap-2">
              <CheckCircle className="w-5 h-5 text-green-600 mt-0.5" />
              <span>고객이 채팅으로 예약을 요청하면 자동으로 등록됩니다</span>
            </li>
            <li className="flex items-start gap-2">
              <CheckCircle className="w-5 h-5 text-green-600 mt-0.5" />
              <span>예약 확정 시 고객에게 자동으로 확인 메시지가 전송됩니다</span>
            </li>
            <li className="flex items-start gap-2">
              <CheckCircle className="w-5 h-5 text-green-600 mt-0.5" />
              <span>예약 시간 1시간 전 알림 메시지가 발송됩니다</span>
            </li>
          </ul>
        </CardContent>
      </Card>
    </div>
  );
}
