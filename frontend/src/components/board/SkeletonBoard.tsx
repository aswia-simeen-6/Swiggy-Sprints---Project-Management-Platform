import { motion } from 'framer-motion';

export default function SkeletonBoard() {
  return (
    <div className="h-full flex flex-col">
      {/* Header skeleton */}
      <div className="px-6 py-4 flex items-center justify-between shrink-0">
        <div className="space-y-2">
          <div className="skeleton h-7 w-48" />
          <div className="skeleton h-4 w-24" />
        </div>
        <div className="skeleton h-10 w-32 rounded-xl" />
      </div>

      {/* Columns skeleton */}
      <div className="flex-1 flex gap-4 px-6 pb-6 overflow-hidden">
        {[0, 1, 2, 3].map((col) => (
          <motion.div
            key={col}
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: col * 0.1 }}
            className="w-[320px] flex-shrink-0 flex flex-col"
          >
            {/* Column header */}
            <div className="flex items-center gap-2 mb-3 px-1">
              <div className="skeleton w-2 h-2 rounded-full" />
              <div className="skeleton h-4 w-24" />
              <div className="skeleton h-5 w-6 rounded-md" />
            </div>

            {/* Cards */}
            <div className="flex-1 rounded-2xl p-2 space-y-2 bg-surface-primary/50">
              {Array.from({ length: 3 - col % 2 }).map((_, i) => (
                <div key={i} className="card p-3.5 space-y-2">
                  <div className="flex items-center gap-2">
                    <div className="skeleton w-3.5 h-3.5 rounded" />
                    <div className="skeleton h-3 w-16" />
                    <div className="skeleton h-4 w-14 rounded-md ml-auto" />
                  </div>
                  <div className="skeleton h-4 w-full" />
                  <div className="skeleton h-4 w-3/4" />
                  <div className="flex items-center gap-1.5 mt-1">
                    <div className="skeleton h-4 w-14 rounded" />
                    <div className="skeleton h-4 w-12 rounded" />
                  </div>
                </div>
              ))}
            </div>
          </motion.div>
        ))}
      </div>
    </div>
  );
}
