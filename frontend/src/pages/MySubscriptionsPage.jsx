import React, { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { format } from "date-fns";
import { ClipboardIcon } from "@heroicons/react/24/outline";
import { toast } from "react-toastify";

import MainLayout from "../layouts/MainLayout";
import BlueBlockLayout from "../layouts/BlueBlockLayout";
import HabitCardForSubscriber from "../components/HabitCardForSubscriber";
import habitsApi from "../api/habitsApi";
import subscriptionsApi from "../api/subscriptionsApi";

const MySubscriptionsPage = () => {
  const navigate = useNavigate();

  const [habits, setHabits] = useState([]);
  const [selectedDate, setSelectedDate] = useState(null);
  const [newHabitId, setNewHabitId] = useState("");

  const location = useLocation();
  const dateFromState = location.state?.selectedDateForHabits;

  useEffect(() => {
    if (dateFromState) {
      setSelectedDate(dateFromState);
    }
  }, [dateFromState]);

  // Хуки для списков подписок
  const [acceptedSubscriptions, setAcceptedSubscriptions] = useState([]);
  const [showAccepted, setShowAccepted] = useState(false);
  const [pendingRequests, setPendingRequests] = useState([]);
  const [showPending, setShowPending] = useState(false);

  const fetchSubscriptions = async () => {
    try {
      const [accepted, pending] = await Promise.all([
        subscriptionsApi.getUserAcceptedSubscriptions(),
        subscriptionsApi.getUserUnprocessedRequests(),
      ]);
      setAcceptedSubscriptions(accepted);
      setPendingRequests(pending);
    } catch (error) {
      console.error("Ошибка при загрузке подписок:", error);
    }
  };

  const fetchHabits = async (date) => {
    try {
      const data = await habitsApi.getAllUserSubscribedHabitsAtDay(date);
      setHabits(data);
    } catch (error) {
      console.error("Ошибка при получении привычек:", error);
    }
  };

  useEffect(() => {
    if (dateFromState) {
      setSelectedDate(dateFromState);
    }

    if (location.state?.selectedDateForHabits) {
      navigate(location.pathname, { replace: true, state: null });
    }

    if (!selectedDate) {
      setSelectedDate(format(new Date(), "yyyy-MM-dd"));
    }

    fetchHabits(selectedDate);
    fetchSubscriptions();
  }, [selectedDate, dateFromState]);

  // При нажатии на кнопку для показа/скрытия необработанных заявок
  const handleTogglePending = () => setShowPending(!showPending);

  // При нажатии на кнопку для показа/скрытия принятых подписок
  const handleToggleAccepted = () => setShowAccepted(!showAccepted);

  // При нажатии на кнопку для отписки
  const handleUnsubscribe = async (habitId) => {
    try {
      await subscriptionsApi.unsubscribe(habitId);
      await new Promise((resolve) => setTimeout(resolve, 100));
      toast.success("Вы отписались от этой привычки");
      fetchHabits(selectedDate);
      fetchSubscriptions();
    } catch (error) {
      toast.error("Ошибка при отписке");
      console.error("Ошибка при отписке от привычки:", error);
    }
  };

  // При нажатии на кнопку для отправки заявки на подписку
  const handleSendRequest = async () => {
    if (newHabitId === "") {
      toast.error("Введите ID привычки");
      return;
    }

    try {
      await subscriptionsApi.sendSubscriptionRequest(Number(newHabitId));
      toast.success("Заявка отправлена");
      setNewHabitId("");
      fetchSubscriptions();
    } catch (error) {
      toast.error(error.message);
      console.error("Ошибка при отправке заявки:", error);
    }
  };

  return (
    <MainLayout>
      <div className="space-y-6">
        <BlueBlockLayout>
          <h2 className="text-lg font-semibold text-gray-800 mb-4">
            Подписаться на привычку
          </h2>
          <div className="flex flex-col sm:flex-row sm:items-center gap-4">
            <input
              type="number"
              min="1"
              value={newHabitId}
              onChange={(e) => setNewHabitId(e.target.value)}
              placeholder="Введите ID привычки"
              className="p-3 border rounded-xl w-full sm:w-64 focus:ring-2 focus:ring-blue-400 focus:outline-none"
            />
            <button
              onClick={handleSendRequest}
              className="px-5 py-3 bg-blue-600 hover:bg-blue-700 text-white text-sm rounded-xl transition"
            >
              Отправить заявку
            </button>
          </div>
        </BlueBlockLayout>

        <BlueBlockLayout>
          <div className="flex items-center justify-between">
            <div className="text-base text-gray-800 flex items-center gap-2">
              Привычки, на которые вы подписаны и приняты:{" "}
              {acceptedSubscriptions.length}
            </div>
            {acceptedSubscriptions.length > 0 && (
              <button
                onClick={handleToggleAccepted}
                className="text-sm text-blue-600 hover:underline"
              >
                {showAccepted ? "Скрыть" : "Показать"}
              </button>
            )}
          </div>

          {showAccepted && acceptedSubscriptions.length > 0 && (
            <div className="mt-2">
              <div className="space-y-2">
                {acceptedSubscriptions.map((sub) => (
                  <div
                    key={sub.habitId}
                    className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 bg-blue-100 border border-blue-300 px-4 py-2 rounded-lg shadow-sm hover:shadow-md transition"
                  >
                    <span className="text-gray-800 text-sm flex items-center gap-2 w-full">
                      <span className="flex-shrink-0 h-5 w-5">
                        <ClipboardIcon className="h-5 w-5 text-blue-500" />
                      </span>
                      <span className="break-words whitespace-pre-wrap [overflow-wrap:anywhere]">
                        {sub.habitName} (ID {sub.habitId})
                      </span>
                    </span>
                    <button
                      onClick={() => handleUnsubscribe(sub.habitId)}
                      className="px-3 py-1 bg-red-500 hover:bg-red-600 text-white text-sm rounded-lg ml-4"
                    >
                      Отписаться
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}
        </BlueBlockLayout>

        <BlueBlockLayout>
          <div className="flex items-center justify-between">
            <div className="text-base text-gray-800 flex items-center gap-2">
              Ваши необработанные заявки: {pendingRequests.length}
            </div>
            {pendingRequests.length > 0 && (
              <button
                onClick={handleTogglePending}
                className="text-sm text-blue-600 hover:underline"
              >
                {showPending ? "Скрыть" : "Показать"}
              </button>
            )}
          </div>

          {showPending && pendingRequests.length > 0 && (
            <div className="mt-2">
              <div className="space-y-2">
                {pendingRequests.map((req) => (
                  <div
                    key={req.habitId}
                    className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 bg-blue-100 border border-blue-300 px-4 py-2 rounded-lg shadow-sm hover:shadow-md transition"
                  >
                    <span className="text-gray-800 text-sm flex items-center gap-2 flex-1 w-full">
                      <span className="flex-shrink-0 h-5 w-5">
                        <ClipboardIcon className="h-5 w-5 text-blue-500" />
                      </span>
                      <p className="break-words whitespace-pre-wrap [overflow-wrap:anywhere]">
                        {req.habitName} (ID {req.habitId})
                      </p>
                    </span>
                    <button
                      onClick={() => handleUnsubscribe(req.habitId)}
                      className="flex-shrink-0 px-3 py-1 bg-red-500 hover:bg-red-600 text-white text-sm rounded-lg ml-4"
                    >
                      Отменить заявку
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}
        </BlueBlockLayout>

        <div className="border-t border-gray-300 my-8" />

        <div className="flex flex-col md:flex-row md:items-end md:justify-between gap-4">
          <div>
            <h3 className="text-xl font-semibold text-gray-800 mb-1">
              Выберите дату
            </h3>
            <input
              type="date"
              value={selectedDate}
              onChange={(e) => setSelectedDate(e.target.value)}
              className="p-3 border rounded-xl focus:ring-2 focus:ring-blue-400 focus:outline-none"
            />
          </div>
        </div>

        {selectedDate && (
          <div>
            <h3 className="text-2xl font-semibold text-gray-800 mb-4">
              Привычки на {format(new Date(selectedDate), "dd.MM.yyyy")} среди
              ваших подписок
            </h3>

            {habits.length === 0 ? (
              <p className="text-gray-600">
                На эту дату не запланировано ни одной привычки, на которую вы
                подписаны
              </p>
            ) : (
              <div className="flex flex-col gap-4">
                {habits.map((habit) => (
                  <HabitCardForSubscriber
                    key={habit.habitId}
                    habit={habit}
                    date={selectedDate}
                  />
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    </MainLayout>
  );
};

export default MySubscriptionsPage;
