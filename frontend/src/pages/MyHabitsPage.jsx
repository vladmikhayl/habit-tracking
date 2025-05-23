import React, { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { format } from "date-fns";

import MainLayout from "../layouts/MainLayout";
import HabitCardForCreator from "../components/HabitCardForCreator";
import NewHabitButton from "../components/buttons/NewHabitButton";
import habitsApi from "../api/habitsApi";

const MyHabitsPage = () => {
  const [habits, setHabits] = useState([]);
  const [selectedDate, setSelectedDate] = useState(null);

  const location = useLocation();
  const dateFromState = location.state?.selectedDateForHabits;

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
    if (dateFromState) {
      setSelectedDate(dateFromState);
    }

    if (location.state?.selectedDateForHabits) {
      navigate(location.pathname, { replace: true, state: null });
    }

    if (!selectedDate) {
      setSelectedDate(format(new Date(), "yyyy-MM-dd"));
    }

    fetchHabits(selectedDate);
  }, [selectedDate, dateFromState]);

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

          <NewHabitButton />
        </div>

        {selectedDate && (
          <div>
            <h3 className="text-2xl font-semibold text-gray-800 mb-4">
              Ваши привычки на {format(new Date(selectedDate), "dd.MM.yyyy")}
            </h3>

            {habits.length === 0 ? (
              <p className="text-gray-600">
                На эту дату не запланировано ни одной привычки
              </p>
            ) : (
              <div className="flex flex-col gap-4">
                {habits.map((habit) => (
                  <HabitCardForCreator
                    key={habit.habitId}
                    habit={habit}
                    date={selectedDate}
                    onReportChange={() => fetchHabits(selectedDate)}
                  />
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    </MainLayout>
  );
};

export default MyHabitsPage;
