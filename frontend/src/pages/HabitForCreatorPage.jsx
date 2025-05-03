import React, { useEffect, useState } from "react";
import { useParams, useNavigate, useLocation } from "react-router-dom";
import MainLayout from "../layouts/MainLayout";
import { InformationCircleIcon } from "@heroicons/react/24/outline";
import { CameraIcon, NoSymbolIcon } from "@heroicons/react/24/outline";
import {
  CheckCircleIcon,
  ClockIcon,
  UserCircleIcon,
  ExclamationCircleIcon,
} from "@heroicons/react/24/solid";

import habitsApi from "../api/habitsApi";
import subscriptionsApi from "../api/subscriptionsApi";
import { toast } from "react-toastify";
import { format } from "date-fns";

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

  const formatFrequency = () => {
    switch (frequencyType) {
      case "WEEKLY_ON_DAYS":
        if (daysOfWeek?.length > 0) {
          const dayNames = {
            MONDAY: "понедельник",
            TUESDAY: "вторник",
            WEDNESDAY: "среда",
            THURSDAY: "четверг",
            FRIDAY: "пятница",
            SATURDAY: "суббота",
            SUNDAY: "воскресенье",
          };
          const dayOrder = [
            "MONDAY",
            "TUESDAY",
            "WEDNESDAY",
            "THURSDAY",
            "FRIDAY",
            "SATURDAY",
            "SUNDAY",
          ];
          const sortedDays = [...daysOfWeek].sort(
            (a, b) => dayOrder.indexOf(a) - dayOrder.indexOf(b)
          );
          return sortedDays.map((day) => dayNames[day]).join(", ");
        }
        return "Нет указанных дней";
      case "WEEKLY_X_TIMES":
        return `${timesPerWeek} раз(а) в неделю`;
      case "MONTHLY_X_TIMES":
        return `${timesPerMonth} раз(а) в месяц`;
      default:
        return "Неизвестно";
    }
  };

  // При нажатии на кнопку для удаления привычки
  const handleDeleteHabit = async () => {
    try {
      const confirm = window.confirm(
        "Вы уверены, что хотите удалить привычку?"
      );
      if (!confirm) return;
      await habitsApi.delete(pageHabitId);
      toast.success("Привычка успешно удалена");
      navigate("/my-habits");
    } catch (error) {
      toast.error("Ошибка при удалении привычки");
      console.error("Ошибка при удалении привычки:", error);
    }
  };

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
        <div className="flex flex-wrap items-center gap-4 mb-4">
          <h1 className="text-3xl font-bold break-words whitespace-pre-wrap [overflow-wrap:anywhere]">
            {name}
          </h1>
          <span className="text-sm font-semibold px-4 py-2 rounded-full bg-blue-100 text-blue-700">
            Это ваша привычка
          </span>
        </div>

        <div
          className={`mb-4 text-base flex items-center gap-2 font-semibold ${
            isPhotoAllowed ? "text-blue-700" : "text-gray-700"
          }`}
        >
          {isPhotoAllowed ? (
            <>
              <CameraIcon className="h-5 w-5 text-blue-600" />
              Привычка с фотоотчётами
            </>
          ) : (
            <>
              <NoSymbolIcon className="h-5 w-5 text-gray-500" />
              Привычка без фотоотчётов
            </>
          )}
        </div>

        <div
          className={`mb-6 text-base flex items-center gap-2 ${
            howManyDaysLeft === null
              ? "text-base text-gray-800 font-semibold"
              : howManyDaysLeft <= 0
              ? "text-green-700 font-semibold"
              : "text-blue-700 font-semibold"
          }`}
        >
          {howManyDaysLeft === null ? (
            <>
              <span className="flex-shrink-0 h-5 w-5">
                <InformationCircleIcon className="h-5 w-5 text-gray-500" />
              </span>
              Длительность привычки не задана
            </>
          ) : howManyDaysLeft <= 0 ? (
            <>
              <span className="flex-shrink-0 h-5 w-5">
                <CheckCircleIcon className="h-5 w-5 text-green-600" />
              </span>
              Привычка завершена: прошло {durationDays} / {durationDays} дней
            </>
          ) : (
            <div className="flex items-center gap-1 text-blue-700">
              <span className="flex-shrink-0 h-5 w-5">
                <ClockIcon className="h-5 w-5 text-blue-600" />
              </span>
              <span className="ml-1">
                Осталось: {howManyDaysLeft} / {durationDays} дней
              </span>
              <div className="relative group">
                <InformationCircleIcon className="h-5 w-5 text-blue-500 cursor-pointer hidden sm:block" />
                <div className="absolute left-6 top-0 w-64 bg-gray-800 text-white text-sm font-normal p-2 rounded-lg shadow-lg opacity-0 group-hover:opacity-100 transition-opacity duration-200 z-10 pointer-events-none">
                  Включая сегодняшний день
                </div>
              </div>
            </div>
          )}
        </div>

        <div className="bg-gray-100 shadow-md rounded-2xl p-6 space-y-4">
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

            <div>
              <span className="text-gray-500">
                {frequencyType === "WEEKLY_ON_DAYS"
                  ? "Запланированные дни выполнения:"
                  : "Частота выполнения:"}
              </span>
              <div className="text-base text-gray-800">{formatFrequency()}</div>
            </div>
          </div>

          {description && description.trim() && (
            <>
              <hr className="my-2 border-gray-300" />
              <div>
                <span className="text-gray-500">Описание:</span>
                <p className="mt-1 text-base text-gray-800 break-words whitespace-pre-wrap [overflow-wrap:anywhere]">
                  {description}
                </p>
              </div>
            </>
          )}
        </div>

        <div className="bg-gray-100 shadow-md rounded-2xl p-6 mt-6 space-y-4">
          <div className="flex items-center justify-between">
            <div className="text-base text-gray-800">
              Принятых подписчиков: {subscribersCount}
            </div>
            {subscribersCount > 0 && (
              <button
                onClick={handleToggleSubscribers}
                className="text-sm text-blue-600 hover:underline"
              >
                {showSubscribers ? "Скрыть" : "Показать"}
              </button>
            )}
          </div>

          {showSubscribers && (
            <div className="mt-2">
              {isLoadingSubscribers ? (
                <div className="text-gray-500 text-sm">Загрузка...</div>
              ) : (
                <div className="bg-gray-50 border border-gray-200 rounded-xl p-4 space-y-2">
                  {subscribers.map((login) => (
                    <div
                      key={login}
                      className="flex items-center gap-3 bg-white px-4 py-2 rounded-lg shadow-sm hover:shadow-md transition"
                    >
                      <UserCircleIcon className="h-6 w-6 text-blue-500" />
                      <span className="text-gray-800">{login}</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>

        <div className="bg-gray-100 shadow-md rounded-2xl p-6 mt-6 space-y-4">
          <div className="flex items-center justify-between flex-wrap">
            <div className="text-base text-gray-800 flex items-center gap-2">
              Необработанных заявок: {pendingRequests.length}
              {pendingRequests.length > 0 && (
                <span className="flex-shrink-0 h-5 w-5">
                  <ExclamationCircleIcon className="h-6 w-6 text-yellow-500" />
                </span>
              )}
            </div>
            {pendingRequests.length > 0 && (
              <button
                onClick={handleTogglePending}
                className="text-sm text-blue-600 hover:underline w-full sm:w-auto mt-2 sm:mt-0"
              >
                {showPending ? "Скрыть" : "Показать"}
              </button>
            )}
          </div>

          {showPending && pendingRequests.length > 0 && (
            <div className="mt-2">
              {isLoadingPending ? (
                <div className="text-gray-500 text-sm">Загрузка...</div>
              ) : (
                <div className="bg-gray-50 border border-gray-200 rounded-xl p-4 space-y-2">
                  {pendingRequests.map((req) => (
                    <div
                      key={req.subscriptionId}
                      className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 bg-white px-4 py-2 rounded-lg shadow-sm hover:shadow-md transition"
                    >
                      <div className="flex items-center gap-3">
                        <UserCircleIcon className="h-6 w-6 text-blue-500" />
                        <span className="text-gray-800">
                          {req.subscriberLogin}
                        </span>
                      </div>
                      <div className="flex gap-2 sm:gap-4 justify-end sm:ml-auto flex-wrap">
                        <button
                          onClick={() => handleAccept(req.subscriptionId)}
                          className="px-3 py-1 bg-green-500 hover:bg-green-600 text-white text-sm rounded-lg w-full sm:w-auto mb-2 sm:mb-0"
                        >
                          Принять
                        </button>
                        <button
                          onClick={() => handleDeny(req.subscriptionId)}
                          className="px-3 py-1 bg-red-500 hover:bg-red-600 text-white text-sm rounded-lg w-full sm:w-auto"
                        >
                          Отклонить
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>

        {reportsInfo && (
          <div className="bg-gray-100 shadow-md rounded-2xl p-6 mt-6 space-y-4">
            <h3 className="text-lg font-semibold mb-4">
              Статистика выполнения
            </h3>

            <div className="bg-white p-4 rounded-lg shadow-sm flex flex-col sm:flex-row sm:justify-between sm:items-center gap-2">
              <span className="text-gray-500">
                Сколько всего раз вы выполнили эту привычку:
              </span>
              <span className="text-gray-800 text-start sm:text-end">
                {reportsInfo.completionsInTotal}
              </span>
            </div>

            {frequencyType === "WEEKLY_ON_DAYS" && (
              <>
                <div className="bg-white p-4 rounded-lg shadow-sm flex flex-col sm:flex-row sm:justify-between sm:items-center gap-2">
                  <span className="text-gray-500">
                    Процент успешных выполнений:
                  </span>
                  <span className="text-gray-800 text-start sm:text-end">
                    {reportsInfo.completionsPercent !== null ? (
                      `${reportsInfo.completionsPercent}% от всех запланированных`
                    ) : (
                      <span className="inline-flex items-center gap-1 relative">
                        —
                        <div
                          className="group relative"
                          onMouseEnter={(e) => {
                            const tooltip =
                              e.currentTarget.querySelector(".tooltip");
                            if (tooltip) {
                              const rect =
                                e.currentTarget.getBoundingClientRect();
                              tooltip.style.top = `${rect.top}px`;
                              tooltip.style.left = `${rect.right + 8}px`;
                            }
                          }}
                        >
                          <InformationCircleIcon className="h-5 w-5 text-blue-500 cursor-pointer hidden sm:block" />
                          <div
                            className="tooltip fixed w-64 bg-gray-800 text-white text-sm p-2 rounded-lg shadow-lg opacity-0 group-hover:opacity-100 transition-opacity duration-200 z-50 pointer-events-none"
                            style={{ top: 0, left: 0 }}
                          >
                            Этот показатель не рассчитывается, если пока не
                            начался ни один день, на который запланирована эта
                            привычка
                          </div>
                        </div>
                      </span>
                    )}
                  </span>
                </div>

                <div className="bg-white p-4 rounded-lg shadow-sm flex flex-col sm:flex-row sm:justify-between sm:items-center gap-2">
                  <span className="text-gray-500">
                    Выполнений подряд в текущей серии:
                  </span>
                  <span className="text-gray-800 text-start sm:text-end inline-flex items-center gap-1 relative">
                    {reportsInfo.serialDays !== null
                      ? reportsInfo.serialDays
                      : "0"}
                    <div
                      className="group relative"
                      onMouseEnter={(e) => {
                        const tooltip =
                          e.currentTarget.querySelector(".tooltip");
                        if (tooltip) {
                          const rect = e.currentTarget.getBoundingClientRect();
                          tooltip.style.top = `${rect.top}px`;
                          tooltip.style.left = `${rect.right + 8}px`;
                        }
                      }}
                    >
                      <InformationCircleIcon className="h-5 w-5 text-blue-500 cursor-pointer hidden sm:block" />
                      <div
                        className="tooltip fixed w-64 bg-gray-800 text-white text-sm p-2 rounded-lg shadow-lg opacity-0 group-hover:opacity-100 transition-opacity duration-200 z-50 pointer-events-none"
                        style={{ top: 0, left: 0 }}
                      >
                        Столько раз подряд вы выполняете эту привычку, не
                        пропустив ни одного запланированного выполнения
                      </div>
                    </div>
                  </span>
                </div>
              </>
            )}

            {(frequencyType === "WEEKLY_X_TIMES" ||
              frequencyType === "MONTHLY_X_TIMES") && (
              <>
                <div className="bg-white p-4 rounded-lg shadow-sm flex flex-col sm:flex-row sm:justify-between sm:items-center gap-2">
                  <span className="text-gray-500">
                    Выполнений за{" "}
                    {frequencyType === "WEEKLY_X_TIMES"
                      ? "текущую неделю"
                      : "текущий месяц"}
                    :
                  </span>
                  <span className="text-gray-800 text-start sm:text-end">
                    {reportsInfo.completionsInPeriod ?? "Не указано"} из{" "}
                    {reportsInfo.completionsPlannedInPeriod ?? "Не указано"}
                  </span>
                </div>
              </>
            )}
          </div>
        )}

        <div className="bg-gray-100 shadow-md rounded-2xl p-6 mt-6 space-y-4">
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
            <div className="bg-gray-50 border p-4 rounded-xl space-y-2">
              <p>
                <span className="font-semibold text-gray-500">Выполнена:</span>{" "}
                {dailyReport.completed ? "Да" : "Нет"}
              </p>
              <p>
                <span className="font-semibold text-gray-500">
                  Дата выполнения:
                </span>{" "}
                {dailyReport.completed
                  ? formatDate(dailyReport.completionTime)
                  : "—"}
              </p>
              {!isPhotoAllowed ? (
                <p>
                  <span className="font-semibold text-gray-500">
                    Фото не требуется
                  </span>
                </p>
              ) : (
                <>
                  <p>
                    <span className="font-semibold text-gray-500">
                      Фото прикреплено:
                    </span>{" "}
                    {!dailyReport.completed
                      ? "—"
                      : dailyReport.photoUrl
                      ? "Да"
                      : "Нет"}
                  </p>
                  {dailyReport.photoUrl && (
                    <img
                      src={dailyReport.photoUrl}
                      alt="Фотоотчёт"
                      className="w-full max-w-xs sm:max-w-sm md:max-w-sm lg:max-w-sm xl:max-w-xs rounded-lg shadow"
                    />
                  )}
                </>
              )}
            </div>
          ) : (
            <></>
          )}
        </div>

        <div className="flex justify-end gap-3 mt-4">
          <button
            onClick={() => navigate(`/habits/${pageHabitId}/edit`)}
            className="px-4 py-2 bg-blue-500 hover:bg-blue-600 text-white rounded-xl text-sm"
          >
            Редактировать привычку
          </button>
          <button
            onClick={handleDeleteHabit}
            className="px-4 py-2 bg-red-500 hover:bg-red-600 text-white rounded-xl text-sm"
          >
            Удалить привычку
          </button>
        </div>
      </div>
    </MainLayout>
  );
};

export default HabitForCreatorPage;
