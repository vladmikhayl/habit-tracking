import React from "react";
import { toast } from "react-toastify";

import reportsApi from "../../api/reportsApi";

const ChangeHabitPhotoButton = ({
  habitId,
  reportId,
  selectedFile,
  onReportChange,
  onPhotoChanged,
}) => {
  const handleChangeHabitPhoto = async () => {
    console.log("Изменяем фото:", habitId, selectedFile);

    if (!selectedFile) {
      toast.error("Файл не выбран");
      return;
    }

    try {
      await reportsApi.changeReportPhoto(
        reportId,
        await reportsApi.uploadFile(selectedFile)
      );
      toast.success("Фото изменено");
      onReportChange();

      await new Promise((resolve) => setTimeout(resolve, 500));
      onPhotoChanged();
    } catch (err) {
      console.error(err);
      toast.error(err.message);
    }
  };

  return (
    <button
      onClick={handleChangeHabitPhoto}
      className="w-full bg-blue-500 hover:bg-blue-600 text-white font-semibold px-5 py-2 rounded-xl transition"
    >
      Добавить фото
    </button>
  );
};

export default ChangeHabitPhotoButton;
