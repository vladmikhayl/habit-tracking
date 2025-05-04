import React from "react";
import { InformationCircleIcon } from "@heroicons/react/24/outline";
import { CheckCircleIcon, ClockIcon } from "@heroicons/react/24/solid";

const HabitDuration = ({ howManyDaysLeft, durationDays }) => {
  return (
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
  );
};

export default HabitDuration;
