import { useState, useRef } from "react";
import React from "react";
import reportsApi from "../api/reportsApi";
import { toast } from "react-toastify";
import { useNavigate } from "react-router-dom";
import { UsersIcon, CalendarIcon } from "@heroicons/react/24/outline";

const HabitCardForCreator = ({ habit, date, onActionComplete }) => {
  const navigate = useNavigate();

  const {
    habitId,
    name,
    isCompleted,
    subscribersCount,
    frequencyType,
    completionsInPeriod,
    completionsPlannedInPeriod,
    isPhotoAllowed,
    isPhotoUploaded,
    reportId,
  } = habit;

  const [selectedFile, setSelectedFile] = useState(null);

  const fileInputRef = useRef(null);

  const getProgressText = () => {
    if (frequencyType === "WEEKLY_X_TIMES") {
      return `За выбранную неделю выполнено ${completionsInPeriod}/${completionsPlannedInPeriod} раз`;
    }
    if (frequencyType === "MONTHLY_X_TIMES") {
      return `За выбранный месяц выполнено ${completionsInPeriod}/${completionsPlannedInPeriod} раз`;
    }
    return null;
  };

  // При прикреплении файла в форму
  const handleFileChange = (e) => {
    setSelectedFile(e.target.files[0]);
  };

  // При нажатии на кнопку для отметки о выполнении
  const handleMarkingHabitCompleted = async () => {
    console.log("Отмечаем как выполненную:", habitId, selectedFile);
    try {
      const photoUrl = selectedFile
        ? "https://i.pinimg.com/736x/b9/a7/55/b9a75516248779bead50d84c52daebf3.jpg"
        : null; // временно photoUrl = ... при создании отчета с фото
      await reportsApi.createReport(habitId, date, photoUrl);
      toast.success("Отметка о выполнении поставлена");
      onActionComplete();

      await new Promise((resolve) => setTimeout(resolve, 500));
      setSelectedFile(null);
    } catch (err) {
      console.error(err);
      toast.error(err.message);
    }
  };

  // При нажатии на кнопку для отмены отметки о выполнении
  const handleCancellingHabitCompletion = async () => {
    console.log("Отменяем выполнение:", habitId);
    try {
      await reportsApi.deleteReport(reportId);
      toast.success("Отметка о выполнении удалена");
      onActionComplete();
    } catch (err) {
      console.error(err);
      toast.error(err.message);
    }
  };

  // При нажатии на кнопку для удаления фото отчета
  const handleDeletingHabitPhoto = async () => {
    console.log("Удаляем фото:", habitId);
    try {
      await reportsApi.changeReportPhoto(reportId, "");
      toast.success("Фото удалено");
      onActionComplete();
    } catch (err) {
      console.error(err);
      toast.error(err.message);
    }
  };

  // При нажатии на кнопку для изменения фото отчета
  const handleChangingHabitPhoto = async () => {
    console.log("Изменяем фото:", habitId, selectedFile);

    if (!selectedFile) {
      toast.error("Файл не выбран");
      return;
    }

    try {
      await reportsApi.changeReportPhoto(
        reportId,
        "https://i.pinimg.com/736x/b9/a7/55/b9a75516248779bead50d84c52daebf3.jpg"
      ); // временно photoUrl = ... при изменении фото
      toast.success("Фото изменено");
      onActionComplete();

      await new Promise((resolve) => setTimeout(resolve, 500));
      setSelectedFile(null);
    } catch (err) {
      console.error(err);
      toast.error(err.message);
    }
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
        <span className="flex-shrink-0 h-5 w-5">
          <UsersIcon className="h-5 w-5 text-blue-500 mr-1" />
        </span>
        Подписчиков: <span className="font-medium">{subscribersCount}</span>
      </div>

      {getProgressText() && (
        <div className="text-gray-700 flex items-center gap-1">
          <span className="flex-shrink-0 h-5 w-5">
            <CalendarIcon className="h-5 w-5 text-blue-500 mr-1" />
          </span>
          {getProgressText()}
        </div>
      )}

      <div className="pt-2 space-y-3">
        {!isCompleted ? (
          // Если отмечена невыполненной
          !isPhotoAllowed ? (
            <button
              onClick={handleMarkingHabitCompleted}
              className="w-full bg-blue-500 hover:bg-blue-600 text-white font-semibold px-5 py-2 rounded-xl transition"
            >
              Отметить как выполненную
            </button>
          ) : !isPhotoUploaded ? (
            <div className="flex flex-col gap-3 border border-gray-300 p-4 rounded-xl bg-gray-50">
              <label className="flex items-center justify-between gap-4 cursor-pointer">
                <div className="flex items-center gap-3 text-gray-700">
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    className="h-6 w-6 text-gray-500"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M15.172 7l-6.586 6.586a2 2 0 002.828 2.828L18 9.828m-3-2.828a4 4 0 015.656 0 4 4 0 010 5.656L12 21H3v-9l9-9a4 4 0 015.656 0z"
                    />
                  </svg>
                  <div className="flex items-center gap-2">
                    <span className="text-sm sm:text-base">
                      {selectedFile
                        ? selectedFile.name
                        : "Это привычка с фотоотчётами. Выберите файл, если хотите прикрепить фото"}
                    </span>
                    {selectedFile && (
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          e.preventDefault();
                          setSelectedFile(null);
                          if (fileInputRef.current) {
                            fileInputRef.current.value = "";
                          }
                        }}
                        className="text-red-500 hover:text-red-700 text-xl leading-none"
                        title="Удалить файл"
                      >
                        ×
                      </button>
                    )}
                  </div>
                </div>
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/*"
                  onChange={handleFileChange}
                  className="hidden"
                />
              </label>
              <button
                onClick={handleMarkingHabitCompleted}
                className="w-full bg-blue-500 hover:bg-blue-600 text-white font-semibold px-5 py-2 rounded-xl transition"
              >
                Отметить как выполненную
              </button>
            </div>
          ) : (
            <></>
          )
        ) : (
          // Если отмечена выполненной
          <div className="space-y-3">
            <div className="flex flex-col sm:flex-row gap-3">
              <button
                onClick={handleCancellingHabitCompletion}
                className="flex-1 bg-yellow-500 hover:bg-yellow-600 text-white font-semibold px-5 py-2 rounded-xl transition"
              >
                Отменить выполнение
              </button>
              {isPhotoUploaded && (
                <button
                  onClick={handleDeletingHabitPhoto}
                  className="flex-1 bg-red-500 hover:bg-red-600 text-white font-semibold px-5 py-2 rounded-xl transition"
                >
                  Удалить фото
                </button>
              )}
            </div>
            {isPhotoAllowed && !isPhotoUploaded && (
              <div className="flex flex-col gap-3 border border-gray-300 p-4 rounded-xl bg-gray-50">
                <label className="flex items-center justify-between gap-4 cursor-pointer">
                  <div className="flex items-center gap-3 text-gray-700">
                    <svg
                      xmlns="http://www.w3.org/2000/svg"
                      className="h-6 w-6 text-gray-500"
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M15.172 7l-6.586 6.586a2 2 0 002.828 2.828L18 9.828m-3-2.828a4 4 0 015.656 0 4 4 0 010 5.656L12 21H3v-9l9-9a4 4 0 015.656 0z"
                      />
                    </svg>

                    <div className="flex items-center gap-2">
                      <span className="text-sm sm:text-base">
                        {selectedFile
                          ? selectedFile.name
                          : "Это привычка с фотоотчётами. Выберите файл, если хотите добавить фото"}
                      </span>
                      {selectedFile && (
                        <button
                          type="button"
                          onClick={(e) => {
                            e.stopPropagation();
                            e.preventDefault();
                            setSelectedFile(null);
                            if (fileInputRef.current) {
                              fileInputRef.current.value = "";
                            }
                          }}
                          className="text-red-500 hover:text-red-700 text-xl leading-none"
                          title="Удалить файл"
                        >
                          ×
                        </button>
                      )}
                    </div>
                  </div>
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/*"
                    onChange={handleFileChange}
                    className="hidden"
                  />
                </label>
                <button
                  onClick={handleChangingHabitPhoto}
                  className="w-full bg-blue-500 hover:bg-blue-600 text-white font-semibold px-5 py-2 rounded-xl transition"
                >
                  Добавить фото
                </button>
              </div>
            )}
          </div>
        )}

        <button
          onClick={() =>
            navigate(`/habits/${habitId}`, {
              state: { selectedDateForHabits: date },
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

export default HabitCardForCreator;
