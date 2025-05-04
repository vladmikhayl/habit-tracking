import React from "react";
import { UserCircleIcon } from "@heroicons/react/24/solid";

const HabitSubscribers = ({
  subscribersCount,
  showSubscribers,
  subscribers,
  isLoadingSubscribers,
  onToggleSubscribers,
}) => {
  return (
    <>
      <div className="flex items-center justify-between">
        <div className="text-base text-gray-800">
          Принятых подписчиков: {subscribersCount}
        </div>
        {subscribersCount > 0 && (
          <button
            onClick={onToggleSubscribers}
            className="text-sm text-blue-600 hover:underline"
          >
            {showSubscribers ? "Скрыть" : "Показать"}
          </button>
        )}
      </div>

      {showSubscribers && (
        <div className="mt-2">
          {isLoadingSubscribers ? (
            <div className="text-gray-500 text-sm">Загрузка...</div>
          ) : (
            <div className="bg-gray-50 border border-gray-200 rounded-xl p-4 space-y-2">
              {subscribers.map((login) => (
                <div
                  key={login}
                  className="flex items-center gap-3 bg-white px-4 py-2 rounded-lg shadow-sm hover:shadow-md transition"
                >
                  <UserCircleIcon className="h-6 w-6 text-blue-500" />
                  <span className="text-gray-800">{login}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </>
  );
};

export default HabitSubscribers;
