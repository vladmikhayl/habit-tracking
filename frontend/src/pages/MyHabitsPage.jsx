import React from "react";
import MainLayout from "../layouts/MainLayout";
import { useState, useEffect } from "react";
import { format } from "date-fns";
import { useNavigate } from "react-router-dom";
import habitsApi from "../api/habitsApi";
import HabitCard from "../components/HabitCard";

const MyHabitsPage = () => {
  const [habits, setHabits] = useState([]);
  const [selectedDate, setSelectedDate] = useState(
    format(new Date(), "yyyy-MM-dd")
  );
  const navigate = useNavigate();

  const fetchHabits = async (date) => {
    try {
      const data = await habitsApi.getAllUserHabitsAtDay(date);
      setHabits(data);
    } catch (error) {
      console.error("Ошибка при получении привычек:", error);
    }
  };

  useEffect(() => {
    fetchHabits(selectedDate);
  }, [selectedDate]);

  return (
    <MainLayout>
      <div className="space-y-8">
        <div className="flex flex-col md:flex-row md:items-end md:justify-between gap-4">
          <div>
            <h3 className="text-xl font-semibold text-gray-700 mb-1">
              Выберите дату
            </h3>
            <input
              type="date"
              value={selectedDate}
              onChange={(e) => setSelectedDate(e.target.value)}
              className="p-3 border rounded-xl focus:ring-2 focus:ring-blue-400 focus:outline-none"
            />
          </div>

          <button
            onClick={() => navigate("/create-habit")}
            className="bg-green-500 hover:bg-green-600 text-white font-semibold px-6 py-3 rounded-xl transition"
          >
            + Новая привычка
          </button>
        </div>

        <div>
          <h3 className="text-xl font-semibold text-gray-700 mb-4">
            Привычки на {format(new Date(selectedDate), "dd.MM.yyyy")}
          </h3>

          {habits.length === 0 ? (
            <p className="text-gray-600">
              На эту дату не запланировано ни одной привычки
            </p>
          ) : (
            <div className="flex flex-col gap-4">
              {habits.map((habit) => (
                <HabitCard
                  key={habit.habitId}
                  habit={habit}
                  date={selectedDate}
                />
              ))}
            </div>
          )}
        </div>
      </div>
    </MainLayout>
  );
};

export default MyHabitsPage;
