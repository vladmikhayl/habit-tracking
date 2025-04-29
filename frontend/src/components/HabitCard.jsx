import React from "react";

const HabitCard = ({ habit, date }) => {
  const {
    habitId,
    name,
    isCompleted,
    subscribersCount,
    frequencyType,
    completionsInPeriod,
    completionsPlannedInPeriod,
    isPhotoAllowed,
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
    <div className="w-full bg-white border border-gray-300 rounded-2xl shadow p-6 space-y-4">
      <div className="flex items-start gap-4 flex-wrap">
        <h4 className="text-xl font-semibold text-gray-800 flex-1 min-w-0 break-words">
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
              isPhotoAllowed
                ? "bg-indigo-100 text-indigo-700"
                : "bg-gray-100 text-gray-600"
            }`}
          >
            {isPhotoAllowed
              ? "Привычка с фотоотчётом"
              : "Привычка без фотоотчёта"}
          </span>
        </div>
      </div>

      <div className="text-gray-700">
        Подписчиков: <span className="font-medium">{subscribersCount}</span>
      </div>

      {getProgressText() && (
        <div className="text-gray-700">{getProgressText()}</div>
      )}

      <div className="pt-2 space-y-3">
        {!isCompleted ? (
          <button className="w-full bg-blue-500 hover:bg-blue-600 text-white font-semibold px-5 py-2 rounded-xl transition">
            Отметить как выполненную
          </button>
        ) : (
          <div className="flex flex-col sm:flex-row gap-3">
            <button className="flex-1 bg-yellow-500 hover:bg-yellow-600 text-white font-semibold px-5 py-2 rounded-xl transition">
              Отменить выполнение
            </button>
            {isPhotoAllowed && (
              <button className="flex-1 bg-purple-500 hover:bg-purple-600 text-white font-semibold px-5 py-2 rounded-xl transition">
                Изменить фото
              </button>
            )}
          </div>
        )}

        <button className="w-full border border-gray-400 text-gray-700 hover:bg-gray-100 font-semibold px-5 py-2 rounded-xl transition">
          Подробнее о привычке
        </button>
      </div>
    </div>
  );
};

export default HabitCard;
