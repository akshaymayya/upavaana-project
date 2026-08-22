import Image from "next/image";
import { ScriptText } from "./ScriptText";

export function ValueProposition() {
  return (
    <section id="start" className="relative px-4 pb-8 pt-6 sm:px-5">
      <div className="relative mb-10 max-w-[510px]">
        <p className="text-[26px] leading-tight text-[#464b44] sm:text-[34.6px]">
          <span className="font-bold italic">Mozaara</span> is AI partner
          <br />
          to manage your{" "}
          <span className="relative inline-block">
            <span className="inline-block w-[113px] border-b-4 border-[#464b44]" />
            <Image
              src="/images/arrow-1.png"
              alt=""
              width={45}
              height={32}
              className="absolute left-2 top-[12px]"
              aria-hidden
            />
            <Image
              src="/images/arrow-2.png"
              alt=""
              width={29}
              height={28}
              className="absolute left-8 top-[24px]"
              aria-hidden
            />
          </span>{" "}
          business
        </p>
        <p className="mt-2 text-right text-[18px] text-[#464b44] sm:text-[20.5px]">
          you can name{" "}
          <ScriptText className="text-[34px] sm:text-[42.5px]">Anything</ScriptText>
        </p>
      </div>

      <div className="grid grid-cols-1 gap-6 sm:grid-cols-3 sm:gap-4">
        <div className="text-[22px] leading-tight text-[#464b44] sm:text-[27.2px]">
          <p>Starting</p>
          <p>a business</p>
        </div>
        <div className="text-[22px] leading-tight text-[#464b44] sm:text-[27.2px]">
          <p>Has never</p>
          <p>been easier.</p>
        </div>
        <div className="text-[22px] leading-tight text-[#464b44] sm:text-[27.2px]">
          <p>Running one</p>
          <p>has never</p>
          <p>been harder.</p>
        </div>
      </div>
    </section>
  );
}
