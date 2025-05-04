import React from "react";
import {
  ExclamationCircleIcon,
  UserCircleIcon,
} from "@heroicons/react/24/solid";

const HabitPendings = ({
  pendingRequests,
  showPending,
  isLoadingPending,
  onTogglePending,
  onAccept,
  onDeny,
}) => {
  return (
    <>
      <div className="flex items-center justify-between flex-wrap">
        <div className="text-base text-gray-800 flex items-center gap-2">
          Необработанных заявок: {pendingRequests.length}
          {pendingRequests.length > 0 && (
            <span className="flex-shrink-0 h-5 w-5">
              <ExclamationCircleIcon className="h-6 w-6 text-yellow-500 relative -top-0.5" />
            </span>
          )}
        </div>
        {pendingRequests.length > 0 && (
          <button
            onClick={onTogglePending}
            className="text-sm text-blue-600 hover:underline w-full sm:w-auto mt-2 sm:mt-0"
          >
            {showPending ? "Скрыть" : "Показать"}
          </button>
        )}
      </div>

      {showPending && pendingRequests.length > 0 && (
        <div className="mt-2">
          {isLoadingPending ? (
            <div className="text-gray-500 text-sm">Загрузка...</div>
          ) : (
            <div className="bg-gray-50 border border-gray-200 rounded-xl p-4 space-y-2">
              {pendingRequests.map((req) => (
                <div
                  key={req.subscriptionId}
                  className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 bg-white px-4 py-2 rounded-lg shadow-sm hover:shadow-md transition"
                >
                  <div className="flex items-center gap-3">
                    <UserCircleIcon className="h-6 w-6 text-blue-500" />
                    <span className="text-gray-800">{req.subscriberLogin}</span>
                  </div>
                  <div className="flex gap-2 sm:gap-4 justify-end sm:ml-auto flex-wrap">
                    <button
                      onClick={() => onAccept(req.subscriptionId)}
                      className="px-3 py-1 bg-green-500 hover:bg-green-600 text-white text-sm rounded-lg w-full sm:w-auto mb-2 sm:mb-0"
                    >
                      Принять
                    </button>
                    <button
                      onClick={() => onDeny(req.subscriptionId)}
                      className="px-3 py-1 bg-red-500 hover:bg-red-600 text-white text-sm rounded-lg w-full sm:w-auto"
                    >
                      Отклонить
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </>
  );
};

export default HabitPendings;
