import React, { useEffect, useState } from "react";
import { useParams, useLocation, useNavigate } from "react-router-dom";
import MainLayout from "../layouts/MainLayout";
import { InformationCircleIcon } from "@heroicons/react/24/outline";
import { CameraIcon, NoSymbolIcon } from "@heroicons/react/24/outline";
import {
  CheckCircleIcon,
  ClockIcon,
  UserCircleIcon,
} from "@heroicons/react/24/solid";
import habitsApi from "../api/habitsApi";
import { format } from "date-fns";

const HabitForSubscriberPage = () => {
  const navigate = useNavigate();
  const { id: pageHabitId } = useParams();

  const [habit, setHabit] = useState(null);
  const [reportsInfo, setReportsInfo] = useState(null);

  // Хуки для показа отчета за выбранный день
  const [selectedDate, setSelectedDate] = useState(
    format(new Date(), "yyyy-MM-dd")
  );
  const [dailyReport, setDailyReport] = useState(null);

  const location = useLocation();
  const { creatorLogin } = location.state || {};
  const selectedDateForHabits =
    location.state?.selectedDateForHabits ?? format(new Date(), "yyyy-MM-dd");

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

  return (
    <MainLayout>
      <div className="max-w-3xl mx-auto p-6">
        <button
          onClick={() =>
            navigate("/my-subscriptions", { state: { selectedDateForHabits } })
          }
          className="mb-4 text-blue-600 hover:underline"
        >
          ← Назад
        </button>
        <div className="flex flex-wrap items-center gap-4 mb-4">
          <h1 className="text-3xl font-bold break-all">{name}</h1>
          <span className="text-sm font-semibold px-4 py-2 rounded-full bg-blue-100 text-blue-700">
            Вы подписаны
          </span>
        </div>

        <div
          className={
            "mb-4 text-base flex items-center gap-2 font-semibold text-blue-700"
          }
        >
          <UserCircleIcon className="h-5 w-5 text-blue-600" />
          Создатель: {creatorLogin}
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
              <InformationCircleIcon className="h-5 w-5 text-gray-500" />
              Длительность привычки не задана
            </>
          ) : howManyDaysLeft <= 0 ? (
            <>
              <CheckCircleIcon className="h-5 w-5 text-green-600" />
              Привычка завершена: прошло {durationDays} / {durationDays} дней
            </>
          ) : (
            <div className="flex items-center gap-1 text-blue-700">
              <ClockIcon className="h-5 w-5 text-blue-600" />
              <span className="ml-1">
                Осталось: {howManyDaysLeft} / {durationDays} дней
              </span>
              <div className="relative group">
                <InformationCircleIcon className="h-5 w-5 text-blue-500 cursor-pointer" />
                <div className="absolute left-6 top-0 w-64 bg-gray-800 text-white text-sm font-normal p-2 rounded-lg shadow-lg opacity-0 group-hover:opacity-100 transition-opacity duration-200 z-10 pointer-events-none">
                  Включая сегодняшний день
                </div>
              </div>
            </div>
          )}
        </div>

        <div className="bg-gray-100 shadow-md rounded-2xl p-6 space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <span className="text-gray-500">ID привычки:</span>
              <div className="flex items-center gap-1">
                <div className="text-base text-gray-800">{id}</div>
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
                <p className="mt-1 text-base text-gray-800 break-words whitespace-pre-wrap">
                  {description}
                </p>
              </div>
            </>
          )}
        </div>

        {reportsInfo && (
          <div className="bg-gray-100 shadow-md rounded-2xl p-6 mt-6 space-y-4">
            <h3 className="text-lg font-semibold mb-4">
              Статистика выполнения
            </h3>

            <div className="bg-white p-4 rounded-lg shadow-sm flex justify-between items-center">
              <span className="text-gray-500">
                Сколько всего раз была выполнена эта привычка:
              </span>
              <span className="text-gray-800">
                {reportsInfo.completionsInTotal}
              </span>
            </div>

            {frequencyType === "WEEKLY_ON_DAYS" && (
              <>
                <div className="bg-white p-4 rounded-lg shadow-sm flex justify-between items-center">
                  <span className="text-gray-500">
                    Процент успешных выполнений:
                  </span>
                  <span className="text-gray-800">
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
                          <InformationCircleIcon className="h-5 w-5 text-blue-500 cursor-pointer" />
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

                <div className="bg-white p-4 rounded-lg shadow-sm flex justify-between items-center">
                  <span className="text-gray-500">
                    Выполнений подряд в текущей серии:
                  </span>
                  <span className="text-gray-800 inline-flex items-center gap-1 relative">
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
                      <InformationCircleIcon className="h-5 w-5 text-blue-500 cursor-pointer" />
                      <div
                        className="tooltip fixed w-64 bg-gray-800 text-white text-sm p-2 rounded-lg shadow-lg opacity-0 group-hover:opacity-100 transition-opacity duration-200 z-50 pointer-events-none"
                        style={{ top: 0, left: 0 }}
                      >
                        Столько раз подряд создатель выполняет эту привычку, не
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
                <div className="bg-white p-4 rounded-lg shadow-sm flex justify-between items-center">
                  <span className="text-gray-500">
                    Выполнений за{" "}
                    {frequencyType === "WEEKLY_X_TIMES"
                      ? "текущую неделю"
                      : "текущий месяц"}
                    :
                  </span>
                  <span className="text-gray-800">
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

          <div className="flex items-center gap-2 mb-4">
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
                      className="max-w-xs rounded-lg shadow"
                    />
                  )}
                </>
              )}
            </div>
          ) : (
            <></>
          )}
        </div>
      </div>
    </MainLayout>
  );
};

export default HabitForSubscriberPage;
