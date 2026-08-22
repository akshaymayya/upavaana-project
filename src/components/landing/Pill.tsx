import { cn } from "@/lib/utils";

type PillProps = {
  children: React.ReactNode;
  className?: string;
  size?: "sm" | "md" | "lg";
};

const sizeClasses = {
  sm: "h-[26px] px-4 text-[9.4px]",
  md: "h-[40px] px-6 text-[32.8px] rounded-[29px]",
  lg: "h-[47px] px-6 text-[37px] rounded-[29px]",
};

export function Pill({ children, className, size = "md" }: PillProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center justify-center bg-[#ffdcdc] font-bold text-[#464b45] whitespace-nowrap",
        sizeClasses[size],
        className
      )}
    >
      {children}
    </span>
  );
}
