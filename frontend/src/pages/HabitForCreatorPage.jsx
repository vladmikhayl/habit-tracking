import React, { useEffect, useState } from "react";
import { useParams, useNavigate, useLocation } from "react-router-dom";
import MainLayout from "../layouts/MainLayout";
import { InformationCircleIcon } from "@heroicons/react/24/outline";

import habitsApi from "../api/habitsApi";
import subscriptionsApi from "../api/subscriptionsApi";
import { toast } from "react-toastify";
import { format } from "date-fns";

import GrayBlockLayout from "../layouts/GrayBlockLayout";
import DeleteHabitButton from "../components/buttons/DeleteHabitButton";
import EditHabitButton from "../components/buttons/EditHabitButton";
import HabitTitle from "../components/habit-page-components/HabitTitle";
import IsPhotoForHabitAllowed from "../components/habit-page-components/IsPhotoForHabitAllowed";
import HabitDuration from "../components/habit-page-components/HabitDuration";
import HabitSubscribers from "../components/habit-page-components/HabitSubscribers";
import HabitPendings from "../components/habit-page-components/HabitPendings";
import HabitFrequency from "../components/habit-page-components/HabitFrequency";
import HabitReportsStats from "../components/habit-page-components/HabitReportsStats";
import HabitDailyReport from "../components/habit-page-components/HabitDailyReport";
import HabitDesctiption from "../components/habit-page-components/HabitDesctiption";

const HabitForCreatorPage = () => {
  const navigate = useNavigate();
  const { id: pageHabitId } = useParams();

  const [habit, setHabit] = useState(null);
  const [reportsInfo, setReportsInfo] = useState(null);

  const location = useLocation();
  const selectedDateForHabits =
    location.state?.selectedDateForHabits ?? format(new Date(), "yyyy-MM-dd");

  // Хуки для показа принятых подписчиков
  const [showSubscribers, setShowSubscribers] = useState(false);
  const [subscribers, setSubscribers] = useState([]);
  const [isLoadingSubscribers, setIsLoadingSubscribers] = useState(false);

  // Хуки для показа необработанных заявок
  const [showPending, setShowPending] = useState(false);
  const [pendingRequests, setPendingRequests] = useState([]);
  const [isLoadingPending, setIsLoadingPending] = useState(false);

  // Хуки для показа отчета за выбранный день
  const [selectedDate, setSelectedDate] = useState(
    format(new Date(), "yyyy-MM-dd")
  );
  const [dailyReport, setDailyReport] = useState(null);

  // Подгрузка основных данных о привычке
  const fetchData = async () => {
    try {
      const data = await habitsApi.getGeneralInfo(pageHabitId);
      setHabit(data);
    } catch (error) {
      console.error("Ошибка при получении привычек:", error);
    }

    try {
      const data = await habitsApi.getReportsInfo(pageHabitId);
      setReportsInfo(data);
    } catch (error) {
      console.error("Ошибка при получении информации об отчетах:", error);
    }

    try {
      const data = await subscriptionsApi.getHabitUnprocessedRequests(
        pageHabitId
      );
      setPendingRequests(data);
    } catch (error) {
      console.error("Ошибка при получении необработанных заявок:", error);
    }
  };

  // Подгрузка данных по отчету за выбранный день
  const fetchReport = async () => {
    try {
      const report = await habitsApi.getReportAtDay(pageHabitId, selectedDate);
      setDailyReport(report);
    } catch (error) {
      setDailyReport(null);
      console.error("Ошибка при получении отчета за день:", error);
    }
  };

  useEffect(() => {
    fetchData();
    fetchReport();
  }, [selectedDate, pageHabitId]);

  if (!habit) {
    return (
      <MainLayout>
        <div className="p-4 text-center">Загрузка...</div>
      </MainLayout>
    );
  }

  const {
    id,
    name,
    description,
    isPhotoAllowed,
    durationDays,
    howManyDaysLeft,
    frequencyType,
    daysOfWeek,
    timesPerWeek,
    timesPerMonth,
    createdAt,
    subscribersCount,
  } = habit;

  const formatDate = (iso) =>
    new Date(iso).toLocaleString("ru-RU", {
      day: "2-digit",
      month: "long",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });

  // При нажатии на кнопку для показа/скрытия принятых подписчиков
  const handleToggleSubscribers = async () => {
    if (!showSubscribers && subscribers.length === 0) {
      setIsLoadingSubscribers(true);
      try {
        const result = await subscriptionsApi.getHabitAcceptedSubscriptions(id);
        setSubscribers(result.map((s) => s.subscriberLogin));
      } catch (error) {
        console.error("Ошибка при получении подписчиков:", error);
      } finally {
        setIsLoadingSubscribers(false);
      }
    }
    setShowSubscribers((prev) => !prev);
  };

  // При нажатии на кнопку для показа/скрытия необработанных заявок
  const handleTogglePending = async () => {
    if (!showPending && pendingRequests.length === 0) {
      setIsLoadingPending(true);
      try {
        const result = await subscriptionsApi.getHabitUnprocessedRequests(id);
        setPendingRequests(result);
      } catch (error) {
        console.error("Ошибка при получении заявок:", error);
      } finally {
        setIsLoadingPending(false);
      }
    }
    setShowPending((prev) => !prev);
  };

  // При нажатии на кнопку для принятия заявки
  const handleAccept = async (subscriptionId) => {
    try {
      await subscriptionsApi.acceptSubscriptionRequest(subscriptionId);
      toast.success("Заявка принята");
      await new Promise((resolve) => setTimeout(resolve, 100));

      setPendingRequests((prev) =>
        prev.filter((req) => req.subscriptionId !== subscriptionId)
      );

      try {
        const result = await subscriptionsApi.getHabitAcceptedSubscriptions(id);
        setSubscribers(result.map((s) => s.subscriberLogin));
      } catch (error) {
        console.error("Ошибка при обновлении подписчиков:", error);
      }

      fetchData();
    } catch (error) {
      toast.error(error.message);
      console.error("Ошибка при принятии заявки:", error);
    }
  };

  // При нажатии на кнопку для отклонения заявки
  const handleDeny = async (subscriptionId) => {
    try {
      await subscriptionsApi.denySubscriptionRequest(subscriptionId);
      toast.success("Заявка отклонена");
      await new Promise((resolve) => setTimeout(resolve, 100));

      setPendingRequests((prev) =>
        prev.filter((req) => req.subscriptionId !== subscriptionId)
      );

      fetchData();
    } catch (error) {
      toast.error(error.message);
      console.error("Ошибка при отклонении заявки:", error);
    }
  };

  return (
    <MainLayout>
      <div className="max-w-3xl mx-auto p-3 sm:p-6">
        <button
          onClick={() =>
            navigate("/my-habits", { state: { selectedDateForHabits } })
          }
          className="mb-4 text-blue-600 hover:underline"
        >
          ← Назад
        </button>

        <HabitTitle name={name} stampText="Это ваша привычка" />

        <IsPhotoForHabitAllowed isPhotoAllowed={isPhotoAllowed} />

        <HabitDuration
          howManyDaysLeft={howManyDaysLeft}
          durationDays={durationDays}
        />

        <GrayBlockLayout>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <span className="text-gray-500">ID привычки:</span>
              <div className="flex items-center gap-1">
                <div className="text-base text-gray-800">{id}</div>
                <div className="relative group">
                  <InformationCircleIcon className="h-5 w-5 text-blue-500 cursor-pointer hidden sm:block" />
                  <div className="absolute left-6 top-0 w-64 bg-gray-800 text-white text-sm p-2 rounded-lg shadow-lg opacity-0 group-hover:opacity-100 transition-opacity duration-200 z-10 pointer-events-none">
                    С помощью этого ID другие люди могут подписаться на вашу
                    привычку
                  </div>
                </div>
              </div>
            </div>

            <div>
              <span className="text-gray-500">Дата создания:</span>
              <div className="text-base text-gray-800">
                {formatDate(createdAt)}
              </div>
            </div>

            <HabitFrequency
              frequencyType={frequencyType}
              daysOfWeek={daysOfWeek}
              timesPerWeek={timesPerWeek}
              timesPerMonth={timesPerMonth}
            />
          </div>

          <HabitDesctiption description={description} />
        </GrayBlockLayout>

        <GrayBlockLayout>
          <HabitSubscribers
            subscribersCount={subscribersCount}
            showSubscribers={showSubscribers}
            subscribers={subscribers}
            isLoadingSubscribers={isLoadingSubscribers}
            onToggleSubscribers={handleToggleSubscribers}
          />
        </GrayBlockLayout>

        <GrayBlockLayout>
          <HabitPendings
            pendingRequests={pendingRequests}
            showPending={showPending}
            isLoadingPending={isLoadingPending}
            onTogglePending={handleTogglePending}
            onAccept={handleAccept}
            onDeny={handleDeny}
          />
        </GrayBlockLayout>

        {reportsInfo && (
          <GrayBlockLayout>
            <HabitReportsStats
              reportsInfo={reportsInfo}
              frequencyType={frequencyType}
            />
          </GrayBlockLayout>
        )}

        <GrayBlockLayout>
          <h3 className="text-lg font-semibold mb-0">История отчётов</h3>

          <div className="flex flex-wrap items-center gap-2 mb-4">
            <p className="text-base text-gray-500 mb-0">Выберите дату:</p>
            <input
              type="date"
              value={selectedDate}
              onChange={(e) => setSelectedDate(e.target.value)}
              className="p-2 border rounded-xl focus:ring-2 focus:ring-blue-400 focus:outline-none"
            />
          </div>

          {new Date(new Date(selectedDate).toDateString()) <
          new Date(new Date(createdAt).toDateString()) ? (
            <p className="text-gray-500">
              В этот день привычка ещё не существовала
            </p>
          ) : dailyReport ? (
            <HabitDailyReport
              dailyReport={dailyReport}
              isPhotoAllowed={isPhotoAllowed}
            />
          ) : (
            <></>
          )}
        </GrayBlockLayout>

        <div className="flex justify-end gap-3 mt-4">
          <EditHabitButton habitId={pageHabitId} />
          <DeleteHabitButton habitId={pageHabitId} />
        </div>
      </div>
    </MainLayout>
  );
};

export default HabitForCreatorPage;
