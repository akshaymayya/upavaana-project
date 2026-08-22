import { cn } from "@/lib/utils";

type ScriptTextProps = {
  children: React.ReactNode;
  className?: string;
  as?: "span" | "p";
};

export function ScriptText({
  children,
  className,
  as: Tag = "span",
}: ScriptTextProps) {
  return (
    <Tag className={cn("font-script leading-none", className)}>{children}</Tag>
  );
}
