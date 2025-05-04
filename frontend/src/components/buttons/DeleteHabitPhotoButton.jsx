import React from "react";
import { toast } from "react-toastify";
import reportsApi from "../../api/reportsApi";

const DeleteHabitPhotoButton = ({ habitId, reportId, onReportChange }) => {
  const handleDeleteHabitPhoto = async () => {
    console.log("Удаляем фото:", habitId);
    try {
      await reportsApi.changeReportPhoto(reportId, "");
      toast.success("Фото удалено");
      onReportChange();
    } catch (err) {
      console.error(err);
      toast.error(err.message);
    }
  };

  return (
    <button
      onClick={handleDeleteHabitPhoto}
      className="flex-1 bg-red-500 hover:bg-red-600 text-white font-semibold px-5 py-2 rounded-xl transition"
    >
      Удалить фото
    </button>
  );
};

export default DeleteHabitPhotoButton;
