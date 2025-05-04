import React from "react";
import { InformationCircleIcon } from "@heroicons/react/24/outline";

const HabitReportsStats = ({ reportsInfo, frequencyType }) => {
  return (
    <>
      <h3 className="text-lg font-semibold mb-4">Статистика выполнения</h3>

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
            <span className="text-gray-500">Процент успешных выполнений:</span>
            <span className="text-gray-800 text-start sm:text-end">
              {reportsInfo.completionsPercent !== null ? (
                `${reportsInfo.completionsPercent}% от всех запланированных`
              ) : (
                <span className="inline-flex items-center gap-1 relative">
                  —
                  <div
                    className="group relative"
                    onMouseEnter={(e) => {
                      const tooltip = e.currentTarget.querySelector(".tooltip");
                      if (tooltip) {
                        const rect = e.currentTarget.getBoundingClientRect();
                        tooltip.style.top = `${rect.top}px`;
                        tooltip.style.left = `${rect.right + 8}px`;
                      }
                    }}
                  >
                    <InformationCircleIcon className="h-5 w-5 text-blue-500 cursor-pointer hidden sm:block" />
                    <div
                      className="tooltip fixed w-64 bg-gray-800 text-white text-sm p-2 rounded-lg shadow-lg opacity-0 group-hover:opacity-100 transition-opacity duration-200 z-50 pointer-events-none text-left"
                      style={{ top: 0, left: 0 }}
                    >
                      Этот показатель не рассчитывается, если пока не начался ни
                      один день, на который запланирована эта привычка
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
              {reportsInfo.serialDays !== null ? reportsInfo.serialDays : "0"}
              <div
                className="group relative"
                onMouseEnter={(e) => {
                  const tooltip = e.currentTarget.querySelector(".tooltip");
                  if (tooltip) {
                    const rect = e.currentTarget.getBoundingClientRect();
                    tooltip.style.top = `${rect.top}px`;
                    tooltip.style.left = `${rect.right + 8}px`;
                  }
                }}
              >
                <InformationCircleIcon className="h-5 w-5 text-blue-500 cursor-pointer hidden sm:block" />
                <div
                  className="tooltip fixed w-64 bg-gray-800 text-white text-sm p-2 rounded-lg shadow-lg opacity-0 group-hover:opacity-100 transition-opacity duration-200 z-50 pointer-events-none text-left"
                  style={{ top: 0, left: 0 }}
                >
                  Столько раз подряд уже выполняется эта привычка, не пропустив
                  ни одного запланированного выполнения
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
    </>
  );
};

export default HabitReportsStats;
