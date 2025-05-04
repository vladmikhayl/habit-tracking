import { useState, useRef } from "react";
import React from "react";
import { useNavigate } from "react-router-dom";
import { UsersIcon, CalendarIcon } from "@heroicons/react/24/outline";
import LearnMoreAboutHabitButton from "./buttons/LearnMoreAboutHabitButton";
import MarkHabitCompletedButton from "./buttons/MarkHabitCompletedButton";
import CancelHabitCompletionButton from "./buttons/CancelHabitCompletionButton";
import DeleteHabitPhotoButton from "./buttons/DeleteHabitPhotoButton";
import ChangeHabitPhotoButton from "./buttons/ChangeHabitPhotoButton";
import FileInput from "./buttons/FileInput";

const HabitCardForCreator = ({ habit, date, onReportChange }) => {
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

  // При нажатии на кнопку для подробностей о привычке
  const handleClickLearnMoreAboutHabit = () =>
    navigate(`/habits/${habitId}`, {
      state: { selectedDateForHabits: date },
    });

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
            <MarkHabitCompletedButton
              habitId={habitId}
              date={date}
              selectedFile={selectedFile}
              onReportChange={onReportChange}
              onPhotoChanged={() => setSelectedFile(null)}
            />
          ) : !isPhotoUploaded ? (
            <div className="flex flex-col gap-3 border border-gray-300 p-4 rounded-xl bg-gray-50">
              <FileInput
                selectedFile={selectedFile}
                fileInputRef={fileInputRef}
                onFileChange={handleFileChange}
                onDeletingSelectedFile={() => {
                  setSelectedFile(null);
                }}
                inputText="Это привычка с фотоотчётами. Выберите файл, если хотите прикрепить фото"
              />
              <MarkHabitCompletedButton
                habitId={habitId}
                date={date}
                selectedFile={selectedFile}
                onReportChange={onReportChange}
                onPhotoChanged={() => setSelectedFile(null)}
              />
            </div>
          ) : (
            <></>
          )
        ) : (
          // Если отмечена выполненной
          <div className="space-y-3">
            <div className="flex flex-col sm:flex-row gap-3">
              <CancelHabitCompletionButton
                habitId={habitId}
                reportId={reportId}
                onReportChange={onReportChange}
              />
              {isPhotoUploaded && (
                <DeleteHabitPhotoButton
                  habitId={habitId}
                  reportId={reportId}
                  onReportChange={onReportChange}
                />
              )}
            </div>
            {isPhotoAllowed && !isPhotoUploaded && (
              <div className="flex flex-col gap-3 border border-gray-300 p-4 rounded-xl bg-gray-50">
                <FileInput
                  selectedFile={selectedFile}
                  fileInputRef={fileInputRef}
                  onFileChange={handleFileChange}
                  onDeletingSelectedFile={() => {
                    setSelectedFile(null);
                  }}
                  inputText="Это привычка с фотоотчётами. Выберите файл, если хотите добавить фото"
                />
                <ChangeHabitPhotoButton
                  habitId={habitId}
                  reportId={reportId}
                  selectedFile={selectedFile}
                  onReportChange={onReportChange}
                  onPhotoChanged={() => setSelectedFile(null)}
                />
              </div>
            )}
          </div>
        )}

        <LearnMoreAboutHabitButton onClick={handleClickLearnMoreAboutHabit} />
      </div>
    </div>
  );
};

export default HabitCardForCreator;
