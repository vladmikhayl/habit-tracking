import { UserIcon, UsersIcon, CalendarIcon } from "@heroicons/react/24/outline";
import { useNavigate } from "react-router-dom";
import React from "react";

const HabitCardForSubscriber = ({ habit, date }) => {
  const navigate = useNavigate();

  const {
    habitId,
    creatorLogin,
    name,
    isCompleted,
    subscribersCount,
    frequencyType,
    completionsInPeriod,
    completionsPlannedInPeriod,
    isPhotoAllowed,
    isPhotoUploaded,
  } = habit;

  const getProgressText = () => {
    if (frequencyType === "WEEKLY_X_TIMES") {
      return `За выбранную неделю выполнено ${completionsInPeriod}/${completionsPlannedInPeriod} раз`;
    }
    if (frequencyType === "MONTHLY_X_TIMES") {
      return `За выбранный месяц выполнено ${completionsInPeriod}/${completionsPlannedInPeriod} раз`;
    }
    return null;
  };

  return (
    <div className="w-full bg-white border-2 border-gray-400 rounded-2xl shadow-lg p-6 space-y-4">
      <div className="flex items-start gap-4 flex-wrap">
        <h4 className="text-xl font-semibold text-gray-700 flex-1 min-w-0 break-words">
          {name}
        </h4>
        <div className="flex flex-wrap gap-2">
          <span
            className={`text-sm font-semibold px-4 py-2 rounded-full ${
              isCompleted
                ? "bg-green-100 text-green-700"
                : "bg-red-100 text-red-700"
            }`}
          >
            {isCompleted ? "Выполнена в этот день" : "Не выполнена в этот день"}
          </span>
          <span
            className={`text-sm font-semibold px-4 py-2 rounded-full ${
              !isPhotoAllowed
                ? "bg-gray-100 text-gray-600"
                : isPhotoUploaded
                ? "bg-green-100 text-green-700"
                : "bg-red-100 text-red-700"
            }`}
          >
            {!isPhotoAllowed
              ? "Фото не требуется"
              : isPhotoUploaded
              ? "Фото прикреплено"
              : "Фото не прикреплено"}
          </span>
        </div>
      </div>

      <div className="text-gray-700 flex items-center gap-1">
        <UserIcon className="h-5 w-5 text-blue-500 mr-1" />
        Создатель привычки: <span className="font-medium">{creatorLogin}</span>
      </div>

      <div className="text-gray-700 flex items-center gap-1">
        <UsersIcon className="h-5 w-5 text-blue-500 mr-1" />
        Подписчиков: <span className="font-medium">{subscribersCount}</span>
      </div>

      {getProgressText() && (
        <div className="text-gray-700 flex items-center gap-1">
          <CalendarIcon className="h-5 w-5 text-blue-500 mr-1" />
          {getProgressText()}
        </div>
      )}

      <div className="pt-2 space-y-3">
        <button
          onClick={() =>
            navigate(`/subscribed-habits/${habitId}`, {
              state: { creatorLogin, selectedDateForHabits: date },
            })
          }
          className="w-full border border-gray-400 text-gray-700 hover:bg-gray-100 font-semibold px-5 py-2 rounded-xl transition"
        >
          Подробнее о привычке
        </button>
      </div>
    </div>
  );
};

export default HabitCardForSubscriber;
