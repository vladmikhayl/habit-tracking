import React, { useState } from "react";
import { toast } from "react-toastify";
import { useNavigate } from "react-router-dom";
import { InformationCircleIcon } from "@heroicons/react/24/outline";

import habitsApi from "../api/habitsApi";
import MainLayout from "../layouts/MainLayout";

const CreateHabitPage = () => {
  const navigate = useNavigate();

  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [frequencyType, setFrequencyType] = useState("weekdays");
  const [weekdays, setWeekdays] = useState([]);
  const [timesCount, setTimesCount] = useState(1);
  const [period, setPeriod] = useState("week");
  const [durationDays, setDurationDays] = useState("");
  const [isPhotoAllowed, SetIsPhotoAllowed] = useState(false);

  const toggleWeekday = (day) => {
    setWeekdays((prev) =>
      prev.includes(day) ? prev.filter((d) => d !== day) : [...prev, day]
    );
  };

  // При нажатии на кнопку для создания привычки
  const handleSubmit = async (e) => {
    e.preventDefault();

    const durationDaysWithNullPossible =
      durationDays === "" ? null : Number(durationDays);

    let frequencyTypeWithPeriod = "";
    let daysOfWeek = null;
    let timesPerWeek = null;
    let timesPerMonth = null;

    if (frequencyType === "weekdays") {
      if (weekdays.length === 0) {
        toast.error("Дни недели не выбраны");
        return;
      }
      frequencyTypeWithPeriod = "WEEKLY_ON_DAYS";
      daysOfWeek = weekdays;
    }
    if (frequencyType === "timesPerPeriod" && period === "week") {
      frequencyTypeWithPeriod = "WEEKLY_X_TIMES";
      timesPerWeek = timesCount;
    }
    if (frequencyType === "timesPerPeriod" && period === "month") {
      frequencyTypeWithPeriod = "MONTHLY_X_TIMES";
      timesPerMonth = timesCount;
    }

    const habitData = {
      name,
      description,
      isPhotoAllowed,
      durationDays: durationDaysWithNullPossible,
      frequencyType: frequencyTypeWithPeriod,
      daysOfWeek,
      timesPerWeek,
      timesPerMonth,
    };

    try {
      await habitsApi.create(habitData);
      console.log("Привычка успешно создана:", habitData);
      toast.success("Привычка успешно создана");
      navigate("/my-habits");
    } catch (err) {
      console.error(err);
      toast.error(err.message);
    }
  };

  return (
    <MainLayout>
      <div className="max-w-3xl mx-auto">
        <form
          onSubmit={handleSubmit}
          className="bg-white rounded-xl shadow-md p-6 sm:p-8 space-y-6"
        >
          <h2 className="text-2xl sm:text-3xl font-bold text-center text-gray-800">
            Создать новую привычку
          </h2>

          <div>
            <label
              htmlFor="name"
              className="block text-base font-medium text-gray-700 mb-1"
            >
              Название *
            </label>
            <input
              id="name"
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
              className="w-full p-3 border rounded-xl focus:ring-2 focus:ring-blue-400 focus:outline-none"
            />
          </div>

          <div>
            <label
              htmlFor="description"
              className="block text-base font-medium text-gray-700 mb-1"
            >
              Описание (необязательно)
            </label>
            <textarea
              id="description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full p-3 border rounded-xl focus:ring-2 focus:ring-blue-400 focus:outline-none"
            />
          </div>

          <div>
            <label className="block text-base font-medium text-gray-700 mb-2">
              Когда нужно будет выполнять эту привычку? *
            </label>
            <div className="space-y-2">
              <label className="flex items-center gap-2">
                <input
                  type="radio"
                  name="frequency"
                  value="weekdays"
                  checked={frequencyType === "weekdays"}
                  onChange={() => setFrequencyType("weekdays")}
                />
                <span className="text-sm">В определённые дни недели</span>
              </label>
              <label className={"flex items-center gap-2"}>
                <input
                  type="radio"
                  name="frequency"
                  value="timesPerPeriod"
                  checked={frequencyType === "timesPerPeriod"}
                  onChange={() => setFrequencyType("timesPerPeriod")}
                />
                <span className={"text-sm flex items-center gap-1"}>
                  Определённое количество раз в неделю или месяц
                </span>
              </label>
            </div>
          </div>

          <div>
            {frequencyType === "weekdays" && (
              <>
                <label className="block text-base font-medium text-gray-700 mt-4 mb-2">
                  Выберите дни недели *
                </label>
                <div className="flex flex-wrap gap-3">
                  {["Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"].map(
                    (day, index) => (
                      <label key={day} className="flex items-center gap-1">
                        <input
                          type="checkbox"
                          checked={weekdays.includes(index)}
                          onChange={() => toggleWeekday(index)}
                        />
                        {day}
                      </label>
                    )
                  )}
                </div>
              </>
            )}

            {frequencyType === "timesPerPeriod" && (
              <>
                <label className="block text-base font-medium text-gray-700 mt-4 mb-2">
                  Укажите количество повторений *
                </label>
                <div className="flex flex-col sm:flex-row items-start sm:items-center gap-3">
                  <input
                    type="number"
                    min="1"
                    value={timesCount}
                    onChange={(e) => setTimesCount(+e.target.value)}
                    className="w-20 p-2 border rounded-xl"
                  />
                  <span>раз(а) в</span>
                  <select
                    value={period}
                    onChange={(e) => setPeriod(e.target.value)}
                    className="p-2 border rounded-xl"
                  >
                    <option value="week">неделю</option>
                    <option value="month">месяц</option>
                  </select>
                </div>
              </>
            )}
          </div>

          <div>
            <label
              htmlFor="durationDays"
              className="flex items-center gap-1 text-base font-medium text-gray-700 mb-1"
            >
              Сколько дней будет длиться эта привычка? (необязательно)
              <div className="relative group">
                <InformationCircleIcon className="h-5 w-5 text-blue-500 cursor-pointer hidden sm:block" />
                <div className="absolute left-6 top-0 w-64 bg-gray-800 text-white text-sm p-2 rounded-lg shadow-lg opacity-0 group-hover:opacity-100 transition-opacity duration-200 z-10 pointer-events-none">
                  <p className="mb-2">
                    Оставьте поле пустым, чтобы не фиксировать длительность
                  </p>
                  <p>Потом длительность можно будет поменять</p>
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

          <div className="flex items-center gap-2">
            <input
              id="isPhotoAllowed"
              type="checkbox"
              checked={isPhotoAllowed}
              onChange={(e) => SetIsPhotoAllowed(e.target.checked)}
            />
            <label
              htmlFor="isPhotoAllowed"
              className="text-gray-700 cursor-pointer"
            >
              К отчётам можно будет прикреплять фото?
            </label>
          </div>

          <button
            type="submit"
            className="w-full bg-blue-500 hover:bg-blue-600 text-white font-semibold text-lg py-3 px-4 rounded-xl transition"
          >
            Создать привычку
          </button>
        </form>
      </div>
    </MainLayout>
  );
};

export default CreateHabitPage;
