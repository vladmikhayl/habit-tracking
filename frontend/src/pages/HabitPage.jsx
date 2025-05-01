import React, { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import MainLayout from "../layouts/MainLayout";
import { InformationCircleIcon } from "@heroicons/react/24/outline";
import {
  CheckCircleIcon,
  ClockIcon,
  UserCircleIcon,
} from "@heroicons/react/24/solid";
import habitsApi from "../api/habitsApi";

const HabitPage = () => {
  const { id: pageHabitId } = useParams();
  const [habit, setHabit] = useState(null);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const data = await habitsApi.getGeneralInfo(pageHabitId);
        setHabit(data);
      } catch (error) {
        console.error("Ошибка при получении привычек:", error);
      }
    };

    fetchData();
  }, [pageHabitId]);

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
    isHarmful,
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
        if (habit.daysOfWeek?.length > 0) {
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
          const sortedDays = [...habit.daysOfWeek].sort(
            (a, b) => dayOrder.indexOf(a) - dayOrder.indexOf(b)
          );
          return sortedDays.map((day) => dayNames[day]).join(", ");
        }
        return "Нет указанных дней";
      case "WEEKLY_X_TIMES":
        return `${habit.timesPerWeek} раз в неделю`;
      case "MONTHLY_X_TIMES":
        return `${habit.timesPerMonth} раз в месяц`;
      default:
        return "Неизвестно";
    }
  };

  return (
    <MainLayout>
      <div className="max-w-3xl mx-auto p-6">
        <div className="flex flex-wrap items-center gap-4 mb-4">
          <h1 className="text-3xl font-bold">{name}</h1>
          <span
            className={`text-sm font-semibold px-4 py-2 rounded-full ${
              isPhotoAllowed
                ? "bg-green-100 text-green-700"
                : "bg-gray-100 text-gray-600"
            }`}
          >
            {isPhotoAllowed
              ? "Привычка с фотоотчётами"
              : "Привычка без фотоотчётов"}
          </span>
        </div>

        <div className="mb-6 text-base flex items-center gap-2 text-blue-700 font-semibold">
          <UserCircleIcon className="h-5 w-5 text-blue-700" />
          Вы — создатель этой привычки
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
            <>
              <ClockIcon className="h-5 w-5 text-blue-600" />
              Осталось: {howManyDaysLeft} / {durationDays} дней
            </>
          )}
        </div>

        <div className="bg-white shadow rounded-2xl p-6 space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <span className="text-gray-500">ID привычки:</span>
              <div className="flex items-center gap-1">
                <div className="text-base text-gray-800">{id}</div>
                <div className="relative group">
                  <InformationCircleIcon className="h-5 w-5 text-blue-500 cursor-pointer" />
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
              <span className="text-gray-500">Тип привычки:</span>
              <div className="text-base text-gray-800 flex items-center gap-1">
                {isHarmful ? "Вредная" : "Регулярная"}
                <div className="relative group">
                  <InformationCircleIcon className="h-5 w-5 text-blue-500 cursor-pointer" />
                  <div className="absolute left-6 top-0 w-64 bg-gray-800 text-white text-sm p-2 rounded-lg shadow-lg opacity-0 group-hover:opacity-100 transition-opacity duration-200 z-10 pointer-events-none">
                    {isHarmful
                      ? "По умолчанию привычка считается выполненной, и в случае срыва нужно снять отметку"
                      : "По умолчанию привычка считается невыполненной, и с указанной регулярностью нужно ставить отметку о выполнении"}
                  </div>
                </div>
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
            <div>
              <span className="text-gray-500">Описание:</span>
              <p className="mt-1 text-base text-gray-800">{description}</p>
            </div>
          )}
        </div>

        <div className="flex justify-end gap-3 mt-4">
          <button className="px-4 py-2 bg-blue-500 hover:bg-blue-600 text-white rounded-xl text-sm">
            Редактировать привычку
          </button>
          <button className="px-4 py-2 bg-red-500 hover:bg-red-600 text-white rounded-xl text-sm">
            Удалить привычку
          </button>
        </div>
      </div>
    </MainLayout>
  );
};

export default HabitPage;
