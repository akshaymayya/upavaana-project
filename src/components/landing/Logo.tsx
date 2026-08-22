import Image from "next/image";

type LogoProps = {
  variant?: "light" | "dark";
  className?: string;
};

export function Logo({ variant = "light", className = "" }: LogoProps) {
  return (
    <Image
      src={variant === "light" ? "/images/logo.png" : "/images/logo-dark.png"}
      alt="Mozaara logo"
      width={28}
      height={32}
      className={className}
    />
  );
}
