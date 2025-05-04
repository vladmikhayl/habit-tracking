import React from "react";
import { toast } from "react-toastify";
import { useNavigate } from "react-router-dom";

import habitsApi from "../../api/habitsApi";

const DeleteHabitButton = ({ habitId }) => {
  const navigate = useNavigate();

  const handleDeleteHabit = async () => {
    try {
      const confirm = window.confirm(
        "Вы уверены, что хотите удалить привычку?"
      );
      if (!confirm) return;
      await habitsApi.delete(habitId);
      toast.success("Привычка успешно удалена");
      navigate("/my-habits");
    } catch (error) {
      toast.error("Ошибка при удалении привычки");
      console.error("Ошибка при удалении привычки:", error);
    }
  };

  return (
    <button
      onClick={handleDeleteHabit}
      className="px-4 py-2 bg-red-500 hover:bg-red-600 text-white rounded-xl text-sm"
    >
      Удалить привычку
    </button>
  );
};

export default DeleteHabitButton;
