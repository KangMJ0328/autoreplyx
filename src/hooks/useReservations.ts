import { useState, useEffect, useCallback } from 'react';
import { reservationsAPI, Reservation, PaginatedResponse } from '../lib/api';

interface UseReservationsParams {
  status?: string;
  dateFrom?: string;
  dateTo?: string;
  page?: number;
  perPage?: number;
}

export function useReservations(params: UseReservationsParams = {}) {
  const [reservations, setReservations] = useState<Reservation[]>([]);
  const [meta, setMeta] = useState({
    currentPage: 1,
    lastPage: 1,
    perPage: 20,
    total: 0,
  });
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchReservations = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);

      const response = await reservationsAPI.list({
        status: params.status,
        date_from: params.dateFrom,
        date_to: params.dateTo,
        page: params.page,
        per_page: params.perPage,
      });

      setReservations(response.data);
      setMeta({
        currentPage: response.meta.current_page,
        lastPage: response.meta.last_page,
        perPage: response.meta.per_page,
        total: response.meta.total,
      });
    } catch (err: any) {
      setError(err.response?.data?.message || '예약 목록을 불러오는데 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  }, [params.status, params.dateFrom, params.dateTo, params.page, params.perPage]);

  useEffect(() => {
    fetchReservations();
  }, [fetchReservations]);

  const updateStatus = async (id: number, status: Reservation['status']) => {
    const updated = await reservationsAPI.updateStatus(id, status);
    setReservations((prev) =>
      prev.map((r) => (r.id === id ? updated : r))
    );
    return updated;
  };

  const addNote = async (id: number, note: string) => {
    const updated = await reservationsAPI.addNote(id, note);
    setReservations((prev) =>
      prev.map((r) => (r.id === id ? updated : r))
    );
    return updated;
  };

  return {
    reservations,
    meta,
    isLoading,
    error,
    fetchReservations,
    updateStatus,
    addNote,
  };
}
