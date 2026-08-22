import { ScriptText } from "./ScriptText";
import { Pill } from "./Pill";

const challengeLine = [
  { text: "Managing payments", highlight: false },
  { text: "marketing", highlight: true },
  { text: "hosting,", highlight: false },
  { text: "customers", highlight: false },
  { text: "pricing,", highlight: false },
  { text: "operations,", highlight: true },
  { text: "analytics...", highlight: false },
];

export function BusinessChallenge() {
  return (
    <section id="features" className="px-4 pb-10 sm:px-5">
      <p className="mb-6 max-w-4xl text-[24px] leading-snug text-[#464b45] sm:text-[32.8px]">
        <ScriptText className="text-[28px] sm:text-[32.8px]">Building</ScriptText> a
        website isn&apos;t difficult anymore.
      </p>

      <div className="mb-8 flex max-w-4xl flex-wrap items-center gap-x-2 gap-y-3 text-[22px] text-[#464b45] sm:text-[32.8px]">
        {challengeLine.map((item) =>
          item.highlight ? (
            <Pill key={item.text} size="md" className="text-[22px] sm:text-[32.8px]">
              {item.text}
            </Pill>
          ) : (
            <span key={item.text}>{item.text}</span>
          )
        )}
      </div>

      <p className="text-[28px] font-bold text-[#464b45] sm:text-[37px]">
        That&apos;s where businesses struggle.
      </p>
    </section>
  );
}
