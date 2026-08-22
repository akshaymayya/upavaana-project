import Image from "next/image";
import { ScriptText } from "./ScriptText";
import { Navbar } from "./Navbar";

export function Hero() {
  return (
    <section className="relative mx-3 mt-3 overflow-hidden rounded-none sm:mx-4">
      <div className="relative h-[320px] overflow-hidden sm:h-[380px] lg:h-[406px]">
        <Image
          src="/images/hero-bg.png"
          alt=""
          fill
          priority
          className="object-cover"
          aria-hidden
        />
        <Image
          src="/images/hero-image.png"
          alt="Sunlit greenhouse landscape"
          fill
          priority
          className="object-cover"
        />
        <div className="absolute inset-0 bg-[rgba(111,90,61,0.2)] opacity-60" />

        <Navbar />

        <div className="absolute inset-x-0 top-[45%] flex flex-col items-center text-center text-white">
          <div className="flex items-end gap-1">
            <ScriptText className="text-[38px] sm:text-[47px]">M</ScriptText>
            <p className="text-[22px] sm:text-[27px]">ake it exist</p>
          </div>
          <p className="mt-2 max-w-[120px] text-[7.8px] leading-snug sm:max-w-none sm:text-[8px]">
            &ldquo;Its not finding the solution
            <br />
            its about finding the question&rdquo;
          </p>
        </div>

        <div className="absolute inset-x-0 bottom-0 mix-blend-color-dodge">
          <Image
            src="/images/mozaara-watermark.png"
            alt="Mozaara"
            width={612}
            height={167}
            className="mx-auto h-auto w-[95%] max-w-[612px]"
          />
        </div>
      </div>

      <div className="pointer-events-none absolute -left-[72px] top-[calc(100%-30px)] hidden w-[751px] sm:block">
        <Image
          src="/images/ellipse-glow.png"
          alt=""
          width={751}
          height={158}
          aria-hidden
        />
      </div>
      <div className="pointer-events-none absolute -left-[152px] top-[calc(100%-20px)] hidden w-[368px] sm:block">
        <Image
          src="/images/ellipse-glow-sm.png"
          alt=""
          width={368}
          height={158}
          aria-hidden
        />
      </div>
      <div className="pointer-events-none absolute right-0 top-[calc(100%-30px)] hidden w-[368px] sm:block">
        <Image
          src="/images/ellipse-glow-sm.png"
          alt=""
          width={368}
          height={158}
          aria-hidden
        />
      </div>
    </section>
  );
}
