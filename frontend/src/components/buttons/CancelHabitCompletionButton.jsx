import React from "react";
import { toast } from "react-toastify";
import reportsApi from "../../api/reportsApi";

const CancelHabitCompletionButton = ({ habitId, reportId, onReportChange }) => {
  const handleCancelHabitCompletion = async () => {
    console.log("Отменяем выполнение:", habitId);
    try {
      await reportsApi.deleteReport(reportId);
      toast.success("Отметка о выполнении удалена");
      onReportChange();
    } catch (err) {
      console.error(err);
      toast.error(err.message);
    }
  };

  return (
    <button
      onClick={handleCancelHabitCompletion}
      className="flex-1 bg-yellow-500 hover:bg-yellow-600 text-white font-semibold px-5 py-2 rounded-xl transition"
    >
      Отменить выполнение
    </button>
  );
};

export default CancelHabitCompletionButton;
