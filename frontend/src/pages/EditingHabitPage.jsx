import React, { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import MainLayout from "../layouts/MainLayout";
import habitsApi from "../api/habitsApi";
import { toast } from "react-toastify";
import { InformationCircleIcon } from "@heroicons/react/24/outline";

const EditingHabitPage = () => {
  const navigate = useNavigate();
  const { id: pageHabitId } = useParams();

  const [description, setDescription] = useState("");
  const [durationDays, setDurationDays] = useState("");

  useEffect(() => {
    const fetchHabitInfo = async () => {
      try {
        const data = await habitsApi.getGeneralInfo(pageHabitId);
        setDescription(data.description || "");
        setDurationDays(data.durationDays?.toString() || "");
      } catch (error) {
        console.error("Ошибка при загрузке данных привычки:", error);
        toast.error("Не удалось загрузить данные привычки");
      }
    };

    if (pageHabitId) {
      fetchHabitInfo();
    }
  }, [pageHabitId]);

  const handleSubmit = async (e) => {
    e.preventDefault();

    try {
      await habitsApi.edit(pageHabitId, {
        description,
        durationDays: durationDays === "" ? 0 : Number(durationDays),
      });
      toast.success("Привычка успешно изменена");
      navigate(`/habits/${pageHabitId}`);
    } catch (err) {
      console.error(err);
      toast.error(err.message);
    }
  };

  return (
    <MainLayout>
      <div className="max-w-3xl mx-auto">
        <button
          onClick={() => navigate(`/habits/${pageHabitId}`)}
          className="mb-4 text-blue-600 hover:underline"
        >
          ← Назад
        </button>

        <form
          onSubmit={handleSubmit}
          className="bg-white rounded-xl shadow-md p-6 sm:p-8 space-y-6"
        >
          <h2 className="text-2xl sm:text-3xl font-bold text-center text-gray-800">
            Редактирование привычки
          </h2>

          <div>
            <label
              htmlFor="description"
              className="block text-base font-medium text-gray-700 mb-1"
            >
              Описание
            </label>
            <textarea
              id="description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full p-3 border rounded-xl focus:ring-2 focus:ring-blue-400 focus:outline-none"
            />
          </div>

          <div>
            <label
              htmlFor="durationDays"
              className="block text-base font-medium text-gray-700 mb-1 flex items-center gap-1"
            >
              Сколько дней длится эта привычка?
              <div className="relative group">
                <InformationCircleIcon className="h-5 w-5 text-blue-500 cursor-pointer hidden sm:block" />
                <div className="absolute left-6 top-0 w-64 bg-gray-800 text-white text-sm p-2 rounded-lg shadow-lg opacity-0 group-hover:opacity-100 transition-opacity duration-200 z-10 pointer-events-none">
                  Оставьте поле пустым, чтобы не фиксировать длительность
                </div>
              </div>
            </label>
            <input
              id="durationDays"
              type="number"
              min="1"
              value={durationDays}
              onChange={(e) => setDurationDays(e.target.value)}
              className="w-full p-3 border rounded-xl focus:ring-2 focus:ring-blue-400 focus:outline-none"
            />
          </div>

          <button
            type="submit"
            className="w-full bg-blue-500 hover:bg-blue-600 text-white font-semibold text-lg py-3 px-4 rounded-xl transition"
          >
            Сохранить изменения
          </button>
        </form>
      </div>
    </MainLayout>
  );
};

export default EditingHabitPage;
