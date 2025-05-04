import React from "react";

const HabitDailyReport = ({ dailyReport, isPhotoAllowed }) => {
  const formatDate = (iso) =>
    new Date(iso).toLocaleString("ru-RU", {
      day: "2-digit",
      month: "long",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });

  return (
    <div className="bg-gray-50 border p-4 rounded-xl space-y-2">
      <p>
        <span className="font-semibold text-gray-500">Выполнена:</span>{" "}
        {dailyReport.completed ? "Да" : "Нет"}
      </p>
      <p>
        <span className="font-semibold text-gray-500">Дата выполнения:</span>{" "}
        {dailyReport.completed ? formatDate(dailyReport.completionTime) : "—"}
      </p>
      {!isPhotoAllowed ? (
        <p>
          <span className="font-semibold text-gray-500">Фото не требуется</span>
        </p>
      ) : (
        <>
          <p>
            <span className="font-semibold text-gray-500">
              Фото прикреплено:
            </span>{" "}
            {!dailyReport.completed ? "—" : dailyReport.photoUrl ? "Да" : "Нет"}
          </p>
          {dailyReport.photoUrl && (
            <img
              src={dailyReport.photoUrl}
              alt="Фотоотчёт"
              className="w-full max-w-xs sm:max-w-sm md:max-w-sm lg:max-w-sm xl:max-w-sm rounded-lg shadow"
            />
          )}
        </>
      )}
    </div>
  );
};

export default HabitDailyReport;
