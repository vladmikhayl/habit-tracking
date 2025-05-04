import React from "react";
import { toast } from "react-toastify";
import reportsApi from "../../api/reportsApi";

const MarkHabitCompletedButton = ({
  habitId,
  date,
  selectedFile,
  onReportChange,
  onPhotoChanged,
}) => {
  const handleMarkHabitCompleted = async () => {
    console.log("Отмечаем как выполненную:", habitId, selectedFile);

    try {
      const photoUrl = selectedFile
        ? await reportsApi.uploadFile(selectedFile)
        : null;

      await reportsApi.createReport(habitId, date, photoUrl);
      toast.success("Отметка о выполнении поставлена");
      onReportChange();

      await new Promise((resolve) => setTimeout(resolve, 500));
      onPhotoChanged();
    } catch (err) {
      console.error("Ошибка при создании отметки о выполнении", err);
      toast.error(err.message);
    }
  };

  return (
    <button
      onClick={handleMarkHabitCompleted}
      className="w-full bg-blue-500 hover:bg-blue-600 text-white font-semibold px-5 py-2 rounded-xl transition"
    >
      Отметить как выполненную
    </button>
  );
};

export default MarkHabitCompletedButton;
