import React, { useEffect, useState } from "react";
import { useParams, useLocation, useNavigate } from "react-router-dom";
import { format } from "date-fns";
import { UserCircleIcon } from "@heroicons/react/24/solid";

import habitsApi from "../api/habitsApi";
import MainLayout from "../layouts/MainLayout";
import GrayBlockLayout from "../layouts/GrayBlockLayout";
import HabitTitle from "../components/habit-page-components/HabitTitle";
import IsPhotoForHabitAllowed from "../components/habit-page-components/IsPhotoForHabitAllowed";
import HabitDuration from "../components/habit-page-components/HabitDuration";
import HabitFrequency from "../components/habit-page-components/HabitFrequency";
import HabitReportsStats from "../components/habit-page-components/HabitReportsStats";
import HabitDailyReport from "../components/habit-page-components/HabitDailyReport";
import HabitDesctiption from "../components/habit-page-components/HabitDesctiption";

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

  return (
    <MainLayout>
      <div className="max-w-3xl mx-auto p-3 sm:p-6">
        <button
          onClick={() =>
            navigate("/my-subscriptions", { state: { selectedDateForHabits } })
          }
          className="mb-4 text-blue-600 hover:underline"
        >
          ← Назад
        </button>

        <HabitTitle name={name} stampText="Вы подписаны" />

        <div
          className={
            "mb-4 text-base flex items-center gap-2 font-semibold text-blue-700"
          }
        >
          <UserCircleIcon className="h-5 w-5 text-blue-600" />
          Создатель: {creatorLogin}
        </div>

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

            <div>
              <span className="text-gray-500">Подписчиков:</span>
              <div className="text-base text-gray-800">{subscribersCount}</div>
            </div>
          </div>

          <HabitDesctiption description={description} />
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
      </div>
    </MainLayout>
  );
};

export default HabitForSubscriberPage;
