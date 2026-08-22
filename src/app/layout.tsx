import type { Metadata } from "next";
import { Luxurious_Script, Outfit } from "next/font/google";
import "./globals.css";

const outfit = Outfit({
  variable: "--font-outfit",
  subsets: ["latin"],
  weight: ["400", "500", "600", "700"],
});

const luxuriousScript = Luxurious_Script({
  variable: "--font-luxurious-script",
  subsets: ["latin"],
  weight: "400",
});

export const metadata: Metadata = {
  title: "Mozaara — AI Partner to Manage Your Business",
  description:
    "Mozaara is an AI partner to manage your business. Make it exist.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body
        className={`${outfit.variable} ${luxuriousScript.variable} antialiased`}
      >
        {children}
      </body>
    </html>
  );
}
