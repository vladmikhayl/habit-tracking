import React from "react";
import MainLayout from "../layouts/MainLayout";
import { useState, useEffect } from "react";
import { format } from "date-fns";
import { useNavigate } from "react-router-dom";
import habitsApi from "../api/habitsApi";
import HabitCardForSubscriber from "../components/HabitCardForSubscriber";

const MySubscriptionsPage = () => {
  const [habits, setHabits] = useState([]);
  const [selectedDate, setSelectedDate] = useState(
    format(new Date(), "yyyy-MM-dd")
  );
  const navigate = useNavigate();

  const fetchHabits = async (date) => {
    try {
      const data = await habitsApi.getAllUserSubscribedHabitsAtDay(date);
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
            <h3 className="text-xl font-semibold text-gray-800 mb-1">
              Выберите дату
            </h3>
            <input
              type="date"
              value={selectedDate}
              onChange={(e) => setSelectedDate(e.target.value)}
              className="p-3 border rounded-xl focus:ring-2 focus:ring-blue-400 focus:outline-none"
            />
          </div>
        </div>

        <div>
          <h3 className="text-2xl font-semibold text-gray-800 mb-4">
            Привычки на {format(new Date(selectedDate), "dd.MM.yyyy")}, на
            которые вы подписаны
          </h3>

          {habits.length === 0 ? (
            <p className="text-gray-600">
              На эту дату не запланировано ни одной привычки, на которую вы
              подписаны
            </p>
          ) : (
            <div className="flex flex-col gap-4">
              {habits.map((habit) => (
                <HabitCardForSubscriber key={habit.habitId} habit={habit} />
              ))}
            </div>
          )}
        </div>
      </div>
    </MainLayout>
  );
};

export default MySubscriptionsPage;
