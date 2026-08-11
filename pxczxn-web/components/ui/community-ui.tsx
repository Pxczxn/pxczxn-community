"use client"

import { createElement, type ComponentProps, type ReactNode } from "react"
import { Badge as ShadcnBadge } from "@/components/ui/badge"
import { Button as ShadcnButton } from "@/components/ui/button"
import { Card as ShadcnCard, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input as ShadcnInput } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { cn } from "@/lib/utils"

type LooseProps = Record<string, unknown> & {
  children?: ReactNode
  className?: string
  style?: Record<string, unknown>
}

export function Button({ children, icon, loading, type, danger, htmlType, block, className, ...props }: LooseProps & { icon?: ReactNode; loading?: boolean; type?: string; danger?: boolean; htmlType?: string; block?: boolean }) {
  return <ShadcnButton variant={danger ? "destructive" : type === "primary" ? "default" : type === "link" ? "link" : "outline"} className={cn("gap-2", block && "w-full", className)} disabled={Boolean(loading || props.disabled)} type={htmlType === "submit" ? "submit" : "button"} {...props as ComponentProps<typeof ShadcnButton>}>{icon}{children}</ShadcnButton>
}

export function Card({ children, title, extra, hoverable, bordered, size, bodyStyle, className, ...props }: LooseProps & { title?: ReactNode; extra?: ReactNode; hoverable?: boolean; bordered?: boolean; size?: string; bodyStyle?: ComponentProps<"div">["style"] }) {
  const hasHeader = Boolean(title || extra)
  return <ShadcnCard className={cn(hoverable && "transition-shadow hover:shadow-md", !hasHeader && "px-(--card-spacing)", className)} {...props as ComponentProps<typeof ShadcnCard>}>
    {hasHeader && <CardHeader className="flex-row items-center justify-between py-4"><CardTitle>{title}</CardTitle>{extra}</CardHeader>}
    {hasHeader ? <CardContent style={bodyStyle as ComponentProps<"div">["style"]}>{children}</CardContent> : children}
  </ShadcnCard>
}

export function Space({ children, className, direction, orientation, size, wrap, ...props }: LooseProps & { direction?: string; orientation?: string; wrap?: boolean }) {
  return <div className={cn("flex flex-wrap items-center gap-2", (direction || orientation) === "vertical" && "flex-col items-stretch", className)} {...props as ComponentProps<"div">}>{children}</div>
}

export function Row({ children, className, gutter, style, ...props }: LooseProps & { gutter?: number | [number, number] }) {
  const gap = Array.isArray(gutter) ? gutter : [gutter, gutter]
  const isCustomGrid = className?.includes("moments-page")
  return <div className={cn(!isCustomGrid && "flex flex-wrap", className)} style={{ columnGap: gap[0] ?? 16, rowGap: gap[1] ?? 16, ...style }} {...props as ComponentProps<"div">}>{children}</div>
}

export function Col({ children, className, span, xs, sm, md, lg, xl, style, ...props }: LooseProps & { span?: number; xs?: number; sm?: number; md?: number; lg?: number; xl?: number }) {
  const columns = xl ?? lg ?? md ?? sm ?? xs ?? span ?? 24
  const width = `${Math.max(1, Math.min(24, columns)) / 24 * 100}%`
  return <div className={className} style={{ boxSizing: "border-box", flex: `0 0 calc(${width} - 16px)`, maxWidth: `calc(${width} - 16px)`, minWidth: 0, ...style }} {...props as ComponentProps<"div">}>{children}</div>
}

export function Badge({ children, count, showZero, className }: LooseProps & { count?: number; showZero?: boolean }) {
  if (typeof count === "number" && children) return <span className="relative inline-flex">{children}{(count || showZero) ? <ShadcnBadge className={cn("absolute -right-2 -top-2 min-w-4 px-1", className)}>{count}</ShadcnBadge> : null}</span>
  if (typeof count === "number") return count || showZero ? <ShadcnBadge className={className}>{count}</ShadcnBadge> : null
  return <ShadcnBadge className={className}>{children}</ShadcnBadge>
}

export function Tag({ children, icon, color, className, ...props }: LooseProps & { icon?: ReactNode; color?: string }) {
  return <ShadcnBadge variant={color ? "default" : "secondary"} className={cn("gap-1", className)} {...props as ComponentProps<typeof ShadcnBadge>}>{icon}{children}</ShadcnBadge>
}

function Title({ level = 2, children, ellipsis, ...props }: LooseProps & { level?: number; ellipsis?: unknown }) {
  return createElement(`h${Math.min(6, Math.max(1, level))}`, props, children)
}

function Paragraph({ children, type, ellipsis, ...props }: LooseProps & { type?: string }) {
  return <p className={cn(type === "secondary" && "text-muted-foreground")} {...props as ComponentProps<"p">}>{children}</p>
}

function Text({ children, type, strong, block, ellipsis, ...props }: LooseProps & { type?: string; strong?: boolean; block?: boolean; ellipsis?: unknown }) {
  return <span className={cn(type === "secondary" && "text-muted-foreground", type === "danger" && "text-destructive", strong && "font-semibold")} {...props as ComponentProps<"span">}>{children}</span>
}

export const Typography = { Title, Paragraph, Text }

export function Empty({ children, description }: LooseProps & { description?: ReactNode }) {
  return <div className="py-10 text-center text-muted-foreground">{description}{children}</div>
}

Empty.PRESENTED_IMAGE_SIMPLE = null

export function Spin({ children, tip, description }: LooseProps & { tip?: ReactNode; description?: ReactNode }) {
  return <div className="py-10 text-center text-muted-foreground">{children || description || tip || "加载中…"}</div>
}

export function Alert({ message, description }: LooseProps & { message?: ReactNode; description?: ReactNode }) {
  return <div role="alert" className="rounded-md border border-destructive/30 bg-destructive/5 p-3 text-destructive">{message}{description}</div>
}

type LegacyInputProps = Omit<ComponentProps<typeof ShadcnInput>, "size" | "prefix"> & { prefix?: ReactNode; suffix?: ReactNode; allowClear?: boolean; size?: string }

export function Input({ prefix, suffix, allowClear, size, ...props }: LegacyInputProps) {
  return <ShadcnInput {...props as ComponentProps<typeof ShadcnInput>} />
}

Input.TextArea = function LegacyTextarea({ autoSize, ...props }: ComponentProps<typeof Textarea> & { autoSize?: boolean | { minRows?: number; maxRows?: number } }) {
  return <Textarea {...props} />
}
Input.Search = ({ enterButton, onSearch, allowClear, ...props }: LooseProps & { enterButton?: ReactNode; onSearch?: (value: string) => void; allowClear?: boolean }) => {
  const value = typeof props.value === "string" ? props.value : ""
  return <div className="flex gap-2"><ShadcnInput {...props as ComponentProps<typeof ShadcnInput>} />{enterButton && <Button type="primary" onClick={() => onSearch?.(value)}>{enterButton === true ? "搜索" : enterButton}</Button>}</div>
}

export function Select({ children, options, onChange, ...props }: LooseProps & { options?: Array<{ value: string; label: ReactNode }>; onChange?: (value: string) => void }) {
  return <select className="h-9 rounded-md border bg-background px-3" onChange={(event) => onChange?.(event.target.value)} {...props as ComponentProps<"select">}>{options?.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}{children}</select>
}

export function Segmented({ options = [], value, onChange, className }: { options?: Array<string | { label: ReactNode; value: string }>; value?: string; onChange?: (value: string) => void; className?: string }) {
  return <div className={cn("inline-flex rounded-md border bg-muted p-1", className)}>{options.map((option) => {
    const item = typeof option === "string" ? { label: option, value: option } : option
    return <button className={cn("rounded px-3 py-1.5 text-sm", value === item.value && "bg-background text-primary shadow-sm")} key={item.value} onClick={() => onChange?.(item.value)}>{item.label}</button>
  })}</div>
}

export function Tabs({ items, activeKey, defaultActiveKey, onChange, className }: { items?: Array<{ key: string; label: ReactNode; children?: ReactNode }>; activeKey?: string; defaultActiveKey?: string; onChange?: (key: string) => void; className?: string }) {
  const active = items?.find((item) => item.key === activeKey || (!activeKey && item.key === defaultActiveKey)) ?? items?.[0]
  return <div className={className}><div className="flex gap-4 border-b">{items?.map((item) => <button className={cn("border-b-2 px-1 py-2", active?.key === item.key ? "border-primary text-primary" : "border-transparent")} onClick={() => onChange?.(item.key)} key={item.key}>{item.label}</button>)}</div>{active?.children}</div>
}

export function Progress({ percent = 0, className, style }: LooseProps & { percent?: number }) {
  return <div className={cn("h-1.5 w-full overflow-hidden rounded-full bg-muted", className)} style={style}><div className="h-full rounded-full bg-primary" style={{ width: `${Math.max(0, Math.min(100, percent))}%` }} /></div>
}

export function Pagination({ current = 1, total = 0, pageSize = 10, onChange, className, showSizeChanger }: LooseProps & { current?: number; total?: number; pageSize?: number; onChange?: (page: number) => void; showSizeChanger?: boolean }) {
  const pages = Math.max(1, Math.ceil(total / pageSize))
  return <nav className={cn("flex items-center gap-2", className)}><Button disabled={current <= 1} onClick={() => onChange?.(current - 1)}>上一页</Button><span className="text-sm text-muted-foreground">{current} / {pages}</span><Button disabled={current >= pages} onClick={() => onChange?.(current + 1)}>下一页</Button></nav>
}

const ListBase = <T,>({ dataSource = [], renderItem, className }: LooseProps & { dataSource?: T[]; renderItem?: (item: T) => ReactNode }) => <div className={cn("divide-y", className)}>{dataSource.map((item, index) => <div key={index}>{renderItem?.(item)}</div>)}</div>

export const List = Object.assign(
  ListBase,
  { Item: Object.assign(({ children, className }: LooseProps) => <div className={cn("py-3", className)}>{children}</div>, { Meta: ({ title, description, avatar }: { title?: ReactNode; description?: ReactNode; avatar?: ReactNode }) => <div className="flex gap-3">{avatar}<div>{title}<div className="text-sm text-muted-foreground">{description}</div></div></div> }) }
)

export function Descriptions({ items = [], className, column }: LooseProps & { items?: Array<{ key: string; label: ReactNode; children: ReactNode }>; column?: number }) {
  return <dl className={cn("grid gap-3", className)}>{items.map((item) => <div key={item.key}><dt className="text-sm text-muted-foreground">{item.label}</dt><dd>{item.children}</dd></div>)}</dl>
}

export function Statistic({ title, value, formatter }: LooseProps & { title?: ReactNode; value?: ReactNode; formatter?: (value: ReactNode) => ReactNode }) {
  return <div><div className="text-sm text-muted-foreground">{title}</div><div className="text-2xl font-semibold">{formatter ? formatter(value) : value}</div></div>
}

export function Result({ status, title, subTitle, extra, className, icon }: LooseProps & { status?: string; title?: ReactNode; subTitle?: ReactNode; extra?: ReactNode; icon?: ReactNode }) {
  return <div className="py-10 text-center"><h2 className="text-xl font-semibold">{title}</h2><p className="mt-2 text-muted-foreground">{subTitle}</p>{extra && <div className="mt-4">{extra}</div>}</div>
}

export function Menu({ items = [], selectedKeys = [], onClick, className, mode }: LooseProps & { items?: Array<{ key: string; icon?: ReactNode; label: ReactNode }>; selectedKeys?: string[]; onClick?: (event: { key: string }) => void; mode?: string }) {
  return <nav className="space-y-1">{items.map((item) => <button key={item.key} className={cn("flex w-full items-center gap-2 rounded-md px-3 py-2 text-left", selectedKeys.includes(item.key) && "bg-primary/10 text-primary")} onClick={() => onClick?.({ key: item.key })}>{item.icon}{item.label}</button>)}</nav>
}

export function Tooltip({ children }: LooseProps) { return <>{children}</> }
export function Drawer({ children, open, title }: LooseProps & { open?: boolean; title?: ReactNode }) { return open ? <aside className="fixed inset-y-0 right-0 z-50 w-full max-w-md overflow-auto border-l bg-background p-5 shadow-xl"><h2 className="mb-4 font-semibold">{title}</h2>{children}</aside> : null }
export function Modal({ children, open, title }: LooseProps & { open?: boolean; title?: ReactNode }) { return open ? <div className="fixed inset-0 z-50 grid place-items-center bg-black/40 p-4"><section className="w-full max-w-lg rounded-lg bg-background p-5 shadow-xl"><h2 className="mb-4 font-semibold">{title}</h2>{children}</section></div> : null }
